# Timetable 비즈니스 정책

- 최초 작성일: 2026-08-09
- 상태: 시간표 세트 생성/목록/상세/수정/삭제 API 확정. 수업 슬롯 등록/목록/상세/수정/삭제 API 확정(수정·삭제는 scope=ALL만 지원). 내보내기(엑셀/PDF/PNG) API 확정.

## 🎯 모듈 책임

- 학원 단위로 관리되는 "시간표 세트"(학기/특강 기간 컨테이너)를 관리한다.
- 세트는 기간(시작~종료일), 운영 시간, 운영 요일, 슬롯 단위(분), 강의실 구성(층별 코드 목록)을 갖는다.
- 세트 안에 실제로 들어가는 개별 수업(수업 슬롯: 요일/시간/강의실/강사/과목)을 등록·조회·수정·삭제한다.

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

## 📚 수업 슬롯

### 필드와 제약

- `classType`(CLASS/SPECIAL/CLINIC/STANDING/EXAM), `dayOfWeek`, `classroomCode`, `startTime`/`endTime`(시작이 종료보다 이전이어야 함)은 필수다.
- `grade`/`teacherName`/`subjectName`은 선택값이다.
- `effectiveFrom`/`effectiveUntil`은 슬롯이 실제로 적용되는 기간이며, 등록 시점에는 항상 소속 시간표 세트의 `startDate`/`endDate`와 같은 값으로 자동 설정된다(사용자가 직접 입력하지 않음).

### 겹침 검사(등록·수정 공통)

- 같은 시간표 세트 + 같은 강의실(`classroomCode`) + 같은 요일(`dayOfWeek`) + 시간대가 겹치고(`start < other.end && other.start < end`) + 적용 기간(`effectiveFrom`~`effectiveUntil`)이 겹치는 기존 슬롯이 있으면 `ClassroomTimeConflictException`(409)을 던져 등록/수정을 거절한다.
- 수정 시에는 수정 대상 슬롯 자기 자신은 겹침 비교에서 제외한다.

### 수정·삭제 적용 범위(scope)

- 화면 명세상 수업 슬롯 수정/삭제는 "해당 회차만"(THIS_OCCURRENCE) / "현재부터 전체"(FROM_NOW) / "전체"(ALL) 3단계 범위를 지원해야 하지만, **현재 구현은 `ALL`만 지원한다.**
- `THIS_OCCURRENCE`/`FROM_NOW`로 요청하면 `UnsupportedSlotScopeException`(400, `TIMETABLE_400_4`)을 던져 명확히 거절한다 — 조용히 무시하거나 `ALL`처럼 동작하지 않는다.
- 이 두 범위를 지원하려면 `timetable_slot_exception` 테이블(날짜별 override/skip 기록)과 `effective_from`/`effective_until` 기간 분할 로직이 필요하며, 마이그레이션(`timetable_slot_exception`)만 미리 준비되어 있고 코드는 후속 범위다.

## 📤 내보내기

- 엑셀(.xlsx)/PDF(A3 가로)/PNG 3개 포맷을 지원한다. 구글 스프레드시트로 저장은 이번 범위가 아니다(Drive API 연동과 계정 결정이 필요한 별도 이슈).
- 3개 포맷 모두 **화면 필터와 무관하게 세트 전체 슬롯**을 요일→시작시각 순으로 정렬한 표(리스트) 형태로 내보낸다. 화면에 보이는 요일×시간 시각적 그리드를 재현하지 않는다.
- 수업종류(`ClassType`) 5개별 배경색은 프론트가 요청 파라미터(`colorClass`/`colorSpecial`/`colorClinic`/`colorStanding`/`colorExam`, 6자리 hex)로 지정하며 백엔드는 팔레트를 계산하지 않고 그대로 적용한다. 형식이 6자리 16진수가 아니면 400(`TIMETABLE_400_5`)으로 거절한다.
- 밀도(행 높이·글자크기)는 반영하지 않는다 — 고정 레이아웃 하나만 지원한다.
- 조회 계열과 동일하게 권한 무관, 같은 학원 소속 인증 사용자면 누구나 호출 가능하다.
- PDF/PNG는 서버가 직접 텍스트를 그려야 해서, 배포 환경(Linux 컨테이너)에 한글 글꼴이 없으면 한글이 깨지는 문제가 실제 기동 테스트로 확인됐다. `src/main/resources/fonts/NanumGothic-*.ttf`(SIL OFL 1.1, 무료 재배포 가능)를 리소스로 포함해 PNG는 `Font.createFont`로 직접 로드하고 PDF는 OpenPDF에 임베드해서, 컨테이너의 시스템 폰트 설치 여부와 무관하게 항상 정상 렌더링되도록 고정했다. 엑셀은 클라이언트(사용자 PC)의 폰트로 렌더링되므로 원래부터 이 문제와 무관하다.

## 🚨 예외 정책

- 도메인 규칙 위반은 `TimetableErrorCode` + 에러별 이름이 드러나는 개별 예외 클래스로 던진다.
- 사용 중인 예외: `TimetableNameRequiredException`(400), `InvalidTimetablePeriodException`(400), `DuplicateClassroomCodeException`(400), `UnsupportedSlotScopeException`(400), `InvalidExportColorException`(400), `TimetableSetNotFoundException`(404), `TimetableSlotNotFoundException`(404), `ClassroomTimeConflictException`(409).
- `docs/ERROR_HANDLING.md`의 표준 패턴을 따르며, `calendar`/`google` 도메인과 동일한 방식이다.
