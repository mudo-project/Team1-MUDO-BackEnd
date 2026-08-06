# Calendar 비즈니스 정책

- 최초 작성일: 2026-08-06
- 상태: 일정 생성·목록/일별 조회·수정 API 확정, 상세조회·삭제는 추후 이슈로 진행

## 🎯 모듈 책임

- 학원 단위로 공유되는 캘린더 일정을 관리한다.
- 사용자·학원의 원본 데이터는 다른 모듈이 소유한다.
- `academy_id`, `created_by`는 값으로만 보관하며, 직접 JPA 연관관계를 맺지 않는다.
- 개인 메모(`memo` 모듈)와 달리 같은 학원 소속 구성원 전체가 열람 대상이다.

## 🔐 접근 권한

### 조회

- 같은 학원 소속으로 인증된 사용자는 학원의 캘린더 일정을 모두 조회할 수 있다.
- 다른 학원의 일정은 조회할 수 없다.

### 작성·수정·삭제

- 기능명세서상 "대표와 대표가 허용한 권한을 가진 사용자"만 작성·수정·삭제할 수 있다.
- `users.role` 값 체계가 자유 텍스트로만 존재하는 현재 시점에는 실제 권한 검사를 구현하지 않는다.
- 지금은 로그인 여부만 검사하며, `CalendarController`에 TODO 주석으로 남긴다.
- `users.role` 체계 확정 후 권한 모듈 연동으로 실제 검증을 반영한다.
- `notice` 도메인과 동일한 결정이다.

## 🗓️ 일정

### 필드와 제약

- `title`은 필수, 최대 200자이며 공백만으로 이루어질 수 없다.
- `content`는 선택, 텍스트(`TEXT`)로 저장한다.
- `event_start_at`은 필수이다.
- `event_end_at`은 선택이다. 값이 있으면 `event_start_at` 이후여야 하며, 이전이면 도메인 규칙 위반으로 거절한다.
- `is_all_day`는 필수, 기본값은 `false`이다. 종일 일정 여부를 나타낸다.
- `color`는 선택, 최대 20자이다. 프론트가 정의한 색상 팔레트 코드를 문자열로 보관한다.
- `created_by`는 요청 본문이 아니라 인증된 사용자(`AuthUser.userId()`)에서 채운다.
- `academy_id`는 요청 본문이 아니라 인증된 사용자(`AuthUser.academyId()`)에서 채운다.

### 시간 관리

- `created_at`, `updated_at`은 `BaseTimeEntity`(Spring Data JPA Auditing)가 저장·수정 시 자동으로 채운다.
- 도메인 모델 `CalendarEvent.create(...)`는 시간 파라미터를 받지 않는다. `Clock`을 애플리케이션 서비스에서 주입받아 도메인에 넘기는 방식(`approval` 패턴)을 쓰지 않는다.
- 이유: 캘린더는 재생성 후 merge하는 특이 케이스가 없는 단순 CRUD이므로 JPA Auditing으로 충분하며, `memo`처럼 Clock 직접 계산이 필요한 요구사항이 없다.

## 🚨 예외 정책

- 도메인 규칙 위반은 `CalendarErrorCode` + 에러별 이름이 드러나는 개별 예외 클래스로 던진다.
- 사용 중인 예외: `CalendarTitleRequiredException`(400), `InvalidCalendarPeriodException`(400), `CalendarEventNotFoundException`(404, `addContext`로 `eventId` 기록).
- `docs/ERROR_HANDLING.md`의 표준 패턴을 따르며, `approval`/`notice`/`memo`의 단일 `<Domain>Exception` 방식은 채택하지 않는다.
- `workspace` 도메인과 동일한 방식이다.
