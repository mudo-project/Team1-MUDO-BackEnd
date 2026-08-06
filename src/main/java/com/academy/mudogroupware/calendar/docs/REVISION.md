# 🔄 Calendar 도메인 리비전 로그

## ✅ 2026-08-06 · 캘린더 도메인 신설 및 일정 생성 API 추가

### 변경 목적

학원 구성원이 공용 캘린더에 회의·행사·일정을 등록해 공유할 수 있는 첫 번째 캘린더 API를 도입했습니다. 다른 API(조회·수정·삭제)를 추가하기 전에 도메인 골격과 예외·문서 규칙을 먼저 확정했습니다.

### 구현 변경

- `calendar` top-level 도메인 모듈을 신설하고, `domain / application / infrastructure / presentation` 4계층 구조로 배치했습니다.
- `CalendarEvent` Domain Model은 private 생성자 + `create(...)` / `restore(...)` 정적 팩토리로만 생성하며, `update(...)` 메서드에서 불변식을 재검증합니다.
- `POST /api/calendars` 엔드포인트를 추가했습니다. 요청은 `title`, `content`, `eventStartAt`, `eventEndAt`, `allDay`, `color`만 받고, `academyId`와 `createdBy`는 `AuthUser`에서 채웁니다.
- 예외 처리는 `docs/ERROR_HANDLING.md`의 표준 패턴(개별 예외 클래스 + `<Domain>ErrorCode` + 공통 예외의 `protected ErrorCode` 생성자)을 채택했습니다. 대다수 도메인(`approval`/`notice`/`memo` 등)이 사용하는 단일 `<Domain>Exception` 방식은 채택하지 않았습니다.
- 영속성 구현체 이름은 `workspace` 선례를 따라 `CalendarEventPersistenceAdapter`로 지정했습니다. 대다수 도메인이 사용하는 `RepositoryImpl` 접미사는 채택하지 않았습니다.
- `CalendarEventEntity`는 `BaseTimeEntity`를 상속해 `created_at`, `updated_at`을 Spring Data JPA Auditing으로 자동 관리합니다. `memo`처럼 애플리케이션이 `Clock`으로 직접 계산하지 않습니다.
- Swagger 어노테이션(`@Tag`, `@Operation`, `@ApiResponses`, `@Schema`)을 처음부터 부착했습니다.
- `be5/V5.1.1__create_calendar_events_table.sql` 마이그레이션을 추가했습니다.

### 유예한 결정

- 기능명세서상 "대표와 대표가 허용한 권한"만 작성·수정·삭제할 수 있어야 하지만, `users.role`이 자유 텍스트인 현재 시점에서는 실제 권한 검사를 구현하지 않았습니다. `CalendarController`에 TODO 주석으로만 남겼고, `users.role` 체계가 확정되면 `@PreAuthorize`로 반영합니다(`notice` 도메인과 동일한 결정).

### 검증

- `CalendarEventTest` — 도메인 `create(...)`/`update(...)`의 정상 케이스와 예외(`CalendarTitleRequiredException`, `InvalidCalendarPeriodException`) 케이스를 검증했습니다.
- `CreateCalendarEventServiceTest` — Command에서 도메인 팩토리를 거쳐 저장까지 이르는 정상 흐름과, 저장된 도메인의 필드 값을 검증했습니다.
- `CalendarControllerTest` — HTTP 계층에서 정상 요청(`201`), Bean Validation 실패(`400`), 미인증(`401`) 응답 형식을 검증했습니다.
- `./gradlew test`(전체) — calendar 신규 테스트 전부 통과, 기존 도메인(`approval`/`notice`/`memo`/`workspace`/`messenger` 등) 회귀 없음을 확인했습니다.
