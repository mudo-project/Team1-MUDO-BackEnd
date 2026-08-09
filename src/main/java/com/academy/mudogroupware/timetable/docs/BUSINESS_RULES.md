# Timetable 비즈니스 정책

- 최초 작성일: 2026-08-09
- 상태: 시간표 세트 생성/목록/상세/수정/삭제 API 확정. 수업 슬롯은 별도 범위(미구현).

## 🎯 모듈 책임

- 학원 단위로 관리되는 "시간표 세트"(학기/특강 기간 컨테이너)를 관리한다.
- 세트는 기간(시작~종료일), 운영 시간, 운영 요일, 슬롯 단위(분), 강의실 구성(층별 코드 목록)을 갖는다.
- 세트 안에 실제로 들어가는 개별 수업(수업 슬롯)은 이번 범위가 아니다.

## 🔐 접근 권한

### 조회

- 같은 학원 소속으로 인증된 사용자는 학원의 모든 시간표 세트를 목록/상세 조회할 수 있다.
- 다른 학원의 세트는 조회할 수 없다(조회 시 `academy_id` 일치 여부를 확인, 불일치 시 404).

### 작성·수정·삭제

- `TIMETABLE:MANAGE` 권한을 보유한 사용자만 가능하다 — 원장 본인, 또는 원장이 역할(role)에 이 권한을 부여한 구성원(위임 가능). 자세한 내용은 [TIMETABLE_PERMISSIONS.md](TIMETABLE_PERMISSIONS.md) 참고.

## 🗓️ 시간표 세트

### 필드와 제약

- `name`은 필수이며, 같은 학원 안에서 유일해야 한다(`uk_timetable_set_academy_name`).
- `startDate`/`endDate`는 필수이며, `endDate`는 `startDate`보다 이전일 수 없다.
- `operatingStartTime`/`operatingEndTime`, `operatingDays`(최소 1개 요일)는 필수다.
- `slotUnitMinutes`는 양수여야 한다(예: 10/30/60).
- `classrooms`는 최소 1개 이상이어야 하며, 세트 내에서 강의실 `code`는 중복될 수 없다(층이 달라도 같은 코드는 허용하지 않는다 — 명세서상 강의실 코드가 물리적 공간을 가리키는 유일 식별자이기 때문).

### 상태 계산

- `status`(PLANNED/ACTIVE/ENDED)는 저장하지 않고, 조회 시점의 오늘 날짜를 `startDate`/`endDate`와 비교해 계산한다.
  - 오늘 < `startDate` → `PLANNED`(예정)
  - `startDate` ≤ 오늘 ≤ `endDate` → `ACTIVE`(진행중)
  - 오늘 > `endDate` → `ENDED`(종료)

## 🚨 예외 정책

- 도메인 규칙 위반은 `TimetableErrorCode` + 에러별 이름이 드러나는 개별 예외 클래스로 던진다.
- 사용 중인 예외: `TimetableNameRequiredException`(400), `InvalidTimetablePeriodException`(400), `DuplicateClassroomCodeException`(400), `TimetableSetNotFoundException`(404).
- `docs/ERROR_HANDLING.md`의 표준 패턴을 따르며, `calendar`/`google` 도메인과 동일한 방식이다.
