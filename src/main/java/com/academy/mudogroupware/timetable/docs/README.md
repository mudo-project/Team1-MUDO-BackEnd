# timetable 모듈

## 책임과 범위

학원의 시간표를 학기/특강 기간 단위(시간표 세트)로 관리한다. 이번 범위는 **시간표 세트 관리**(생성/조회/수정/삭제), **수업 슬롯 관리**(등록/목록·상세 조회/수정/삭제, 강의실·요일·시간 겹침 검사), **내보내기**(엑셀/PDF/PNG)까지다. 수업 슬롯 수정·삭제의 3단계 적용 범위(해당 회차만/현재부터 전체/전체) 중 현재는 "전체(ALL)"만 지원하며, 나머지 두 범위는 요청 시 400으로 명시적으로 거절한다(후속 이슈). 내보내기는 구글 스프레드시트로 저장을 제외한 3개 포맷만 지원한다(후속 이슈).

- 조회는 같은 학원 소속 인증 사용자 전체에게 열려 있다.
- 작성/수정/삭제는 `TIMETABLE:MANAGE` 권한(원장 + 원장이 위임한 구성원)만 가능하다. 캘린더(`calendar`) 도메인과 동일한 패턴이다.

## 담당자

- 담당자번호 `be5` (캘린더/구글 연동과 동일 담당자).

## 다른 도메인과의 관계

- `lecture` 도메인이 강의실(`classroom`), 학기(`term`), 강사/과목/시간표(요일·시간) 개념을 이미 갖고 있어 이번 시간표 세트 기능과 데이터 모델이 상당 부분 겹친다. 중복을 피하려면 `lecture`를 확장(수정/삭제 UseCase 추가, 수업 종류 필드 추가, `Term`에 기간 컬럼 추가 등)하는 편이 나을 수 있으나, 이번 범위에서는 완전히 독립된 `timetable` 도메인으로 구현했다. `lecture` 담당자에게 통합 여부 검토를 별도로 요청해뒀다(진행 중).
- `timetable_set_classroom`은 `lecture`의 `classroom` 마스터 테이블을 참조하지 않는 별도의, 시간표 세트 전용 강의실 코드 목록이다(세트를 만들 때마다 직접 입력).
- `timetable_slot`도 마찬가지로 `lecture_schedule`을 참조하지 않는 독립 테이블이다.

## 소유하는 주요 데이터와 상태

- `TimetableSet` — DB 테이블 `timetable_set`(name(테넌트 DB 내 유일), start_date, end_date, operating_start_time, operating_end_time, operating_days(콤마 구분 요일 문자열), slot_unit_minutes, created_at, updated_at)
- `TimetableClassroom` — DB 테이블 `timetable_set_classroom`(timetable_set_id, floor, code — 세트 내 code 유일). JPA `@ElementCollection`으로 매핑, 별도 Entity/Repository 없음.
- `TimetableSlot` — DB 테이블 `timetable_slot`(timetable_set_id, class_type, day_of_week, classroom_code, start_time, end_time, grade, teacher_name, subject_name, effective_from, effective_until, created_at, updated_at). `effective_from`/`effective_until`은 현재 항상 소속 세트의 `start_date`/`end_date`와 같다(회차 단위 기간 분할은 아직 미구현).
- `timetable_slot_exception` 테이블은 "해당 회차만" 적용 범위를 위해 마이그레이션만 미리 만들어 뒀고, 이번 범위의 코드에서는 사용하지 않는다.
- 상태(`TimetableSetStatus`: PLANNED/ACTIVE/ENDED)는 저장하지 않고 조회 시점에 `start_date`/`end_date`와 오늘 날짜를 비교해 계산한다(캘린더/구글 연동과 동일한 "계산된 상태" 패턴).
- `created_at`/`updated_at`은 `global.infrastructure.persistence.BaseTimeEntity`(JPA Auditing)로 자동 관리한다.

## 외부에 공개하는 Application API

- `CreateTimetableSetUseCase` — 세트 생성
- `GetTimetableSetsUseCase` — 세트 목록 조회
- `GetTimetableSetUseCase` — 세트 상세 조회
- `UpdateTimetableSetUseCase` — 세트 수정
- `DeleteTimetableSetUseCase` — 세트 삭제
- `CreateTimetableSlotUseCase` — 수업 슬롯 등록(강의실/요일/시간 겹침 검사 포함)
- `GetTimetableSlotsUseCase` — 수업 슬롯 목록 조회
- `GetTimetableSlotUseCase` — 수업 슬롯 상세 조회
- `UpdateTimetableSlotUseCase` — 수업 슬롯 수정(scope=ALL만 지원)
- `DeleteTimetableSlotUseCase` — 수업 슬롯 삭제(scope=ALL만 지원)
- `ExportTimetableUseCase` — 시간표 세트 전체를 엑셀/PDF/PNG로 내보내기
- 현재 다른 도메인이 소비하는 Port는 없다.

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- 현재 없음.

## 변경 시 주의 사항

- 도메인 규칙 위반은 `timetable.domain.exception.TimetableErrorCode` + 에러별 예외 클래스(`TimetableNameRequiredException` 등)로 던진다. `calendar` 도메인과 동일한 패턴이다.
- 세트 삭제 시 `timetable_set_classroom`, `timetable_slot`(그리고 `timetable_slot_exception`)이 모두 `ON DELETE CASCADE`로 함께 삭제된다.
- 수업 슬롯 수정/삭제는 `UpdateScope` 파라미터를 받지만 `ALL`만 실제로 처리하며, `THIS_OCCURRENCE`/`FROM_NOW`는 `UnsupportedSlotScopeException`(400, `TIMETABLE_400_4`)으로 명시적으로 거절한다. 이 정책을 바꾸려면 `timetable_slot_exception` 테이블과 기간 분할 로직을 함께 구현해야 한다.
- 마이그레이션 담당자번호는 `be5` (`V5.1.3`~`V5.1.5`).

## 세부 문서

- [BUSINESS_RULES.md](BUSINESS_RULES.md) — 도메인 정책과 접근 권한, 검증 규칙
- [TIMETABLE_API.md](TIMETABLE_API.md) — 시간표 세트 엔드포인트별 요청·응답·에러 코드
- [TIMETABLE_SLOT_API.md](TIMETABLE_SLOT_API.md) — 수업 슬롯 엔드포인트별 요청·응답·에러 코드
- [TIMETABLE_EXPORT_API.md](TIMETABLE_EXPORT_API.md) — 내보내기 엔드포인트 요청·응답·에러 코드
- [TIMETABLE_PERMISSIONS.md](TIMETABLE_PERMISSIONS.md) — 권한 코드 정의
