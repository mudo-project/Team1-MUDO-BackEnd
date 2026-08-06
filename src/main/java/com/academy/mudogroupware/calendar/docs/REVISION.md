# 🔄 Calendar 도메인 리비전 로그

## ✅ 2026-08-07 · 목록/일별/월간 조회 쿼리를 date/yearMonth 기반으로 변경

### 변경 목적

프론트 UI가 일별/월별 단위로 캘린더를 탐색하는데, 기존 `from`/`to`(LocalDateTime) 방식은 프론트가 "그날 00:00:00~23:59:59", "그달 1일 00:00:00~말일 23:59:59"를 직접 계산해서 보내야 했습니다. 이 계산을 서버로 옮겨 계약을 단순화했습니다.

### 구현 변경

- `CalendarController.getEvents`가 `from`/`to` 대신 `date`(`LocalDate`, 일별) 또는 `yearMonth`(`YearMonth`, 월간, `@DateTimeFormat(pattern = "yyyy-MM")`)를 받습니다. 정확히 하나만 지정해야 하며, 위반 시 새로 추가한 `InvalidCalendarQueryException`(`CALENDAR_400_3`)을 던집니다.
- 구간 계산(`date.atStartOfDay()` ~ `date.atTime(LocalTime.MAX)`, 또는 `yearMonth.atDay(1).atStartOfDay()` ~ `yearMonth.atEndOfMonth().atTime(LocalTime.MAX)`)은 Controller에서 수행합니다. 애플리케이션 전체가 이미 한국 시간(Asia/Seoul) 기준 `LocalDateTime`을 그대로 다룬다는 전제(`docs/DATABASE.md`)를 따르므로 별도의 타임존 변환 로직은 추가하지 않았습니다.
- `GetCalendarEventsUseCase`/`GetCalendarEventsService`는 시그니처(`academyId`, `from`, `to`)를 그대로 유지합니다. 계약 단순화는 표현 계층(Controller)의 책임으로 한정하고, 애플리케이션 계층은 손대지 않았습니다.
- `CalendarErrorCode.INVALID_QUERY_RANGE`(`CALENDAR_400_3`)를 추가했습니다.

### 유예한 결정

- `GetCalendarEventsService`의 `to.isBefore(from)` 방어 로직은 그대로 남겨뒀습니다. 이 API 경로에서는 Controller가 항상 유효한 구간을 계산해 넘기므로 사실상 도달하지 않지만, Service 자체의 불변식으로서는 여전히 유효합니다.

### 검증

- `CalendarControllerTest` — `date`만 지정, `yearMonth`만 지정, 둘 다 지정, 둘 다 생략, 미인증 각각의 응답을 검증했습니다. `LocalTime.MAX`(`23:59:59.999999999`) 기준으로 계산되는 정확한 종료 시각까지 확인했습니다.
- `./gradlew test`(전체) — calendar 관련 테스트 전부 통과, 기존 도메인 회귀 없음을 확인했습니다.

## ✅ 2026-08-06 · 일정 수정 API 추가

### 변경 목적

목록/일별 조회 다음으로, 등록된 일정의 필드를 수정하는 API를 추가했습니다.

### 구현 변경

- `PATCH /api/calendars/{eventId}`를 추가했습니다. 요청 필드는 생성 API와 동일하며, 부분 필드가 아니라 전체 필드를 매번 새 값으로 교체합니다(PATCH의 일반적 부분수정 의미가 아님을 문서에 명시).
- `UpdateCalendarEventService`는 `findById` 후 요청자의 `academyId`와 다르면(또는 존재하지 않으면) 동일하게 `CalendarEventNotFoundException`(`CALENDAR_404_1`)을 던집니다. 상세조회와 같은 결정입니다.
- 조회한 도메인 객체의 `update(...)`를 호출해 불변식을 재검증한 뒤 `save(event)`로 저장합니다.
- **`CalendarEventPersistenceAdapter.save(...)`를 수정했습니다.** 기존에는 항상 `toEntity(...)`로 새 Entity를 만들어 저장했는데, update 시 이 방식을 그대로 쓰면 새 Entity의 `createdAt`이 비어 있어 JPA가 기존 `created_at`을 `NULL`로 덮어써 `NOT NULL` 제약을 위반합니다. `rollcall`의 `MessageTemplateRepositoryImpl` 선례를 따라, `calendarEvent.getId() != null`이면 `CalendarEventJpaRepository.getReferenceById(...)`로 관리 상태의 Entity를 가져와 새로 추가한 `CalendarEventEntity.update(...)` mutator로 필드만 갱신하도록 분기했습니다. 생성 흐름(`id == null`)은 기존 `toEntity(...)` 경로 그대로라 영향이 없습니다.
- `CalendarEventEntity`에 `update(title, content, eventStartAt, eventEndAt, allDay, color)` mutator를 추가했습니다(entity는 Builder 생성자만 갖던 기존 구조에 update만 추가, setter 전체 노출은 하지 않음).
- 응답은 `memo`의 PATCH 엔드포인트들과 동일하게 본문 없이 `204 No Content`를 반환합니다. 새 `ResponseCode`를 추가하지 않았습니다.

### 유예한 결정

- 상세조회(`GET /api/calendars/{eventId}`)는 별도 브랜치(`feature/calendar-detail-query`)에서 진행 중이며, 삭제(`DELETE`)는 다음 이슈에서 진행합니다.

### 검증

- `UpdateCalendarEventServiceTest` — 정상 수정 흐름(필드 반영 확인), 존재하지 않는 일정, 다른 학원 소속 일정, 도메인 검증 실패(공백 제목) 각각에서 예상대로 동작/예외가 발생하고 `save`가 호출되지 않는지 검증했습니다.
- `CalendarControllerTest` — `PATCH /api/calendars/{eventId}`의 `204`/`400`/`404`/`401` 응답 형식을 검증했습니다. 검증 과정에서 요청 본문에 `allDay`가 없으면 Bean Validation이 아니라 Jackson 역직렬화 실패(`HttpMessageNotReadableException`)로 400이 나는 것을 발견해, 테스트 본문에 `allDay`를 명시적으로 포함시켰습니다(같은 패턴이 기존 생성 API 테스트에도 있어 별도 점검 작업으로 분리했습니다).
- `./gradlew test`(전체) — calendar 관련 테스트 전부 통과, 기존 도메인 회귀 없음을 확인했습니다.

## ✅ 2026-08-06 · 일정 목록/일별 조회 API 추가

### 변경 목적

일정 생성 다음으로, 학원 구성원이 등록된 일정을 기간 단위(월간 목록/일별)로 조회할 수 있는 API를 추가했습니다.

### 구현 변경

- `GET /api/calendars?from=&to=`를 추가했습니다. 목록조회와 일별조회를 하나의 엔드포인트로 겸용합니다(일별조회는 `from`/`to`에 같은 날의 00:00:00~23:59:59를 넣어 호출).
- 조회는 Command 객체 없이 `academyId`, `from`, `to`를 그대로 UseCase에 전달합니다. `memo` 도메인의 조회 패턴(Domain Model을 그대로 반환, 별도 View 불필요)을 따랐습니다.
- `GetCalendarEventsService`는 `@Transactional(readOnly = true)`로 선언했습니다.
- `to`가 `from`보다 이전이면 리포지토리를 조회하지 않고 기존 `InvalidCalendarPeriodException`(`CALENDAR_400_2`)을 재사용합니다. 조회 전용 새 에러 코드를 추가하지 않았습니다(의미가 동일하기 때문).
- `CalendarEventResponse`를 새로 추가해 목록/일별/상세 조회에서 공용으로 재사용하도록 설계했습니다(계획 문서의 "과설계 방지" 원칙).
- 조회 대상은 `AuthUser.academyId()`로 한정하며, 다른 학원의 일정은 응답에 포함되지 않습니다.

### 유예한 결정

- 상세조회(`GET /api/calendars/{eventId}`), 수정(`PATCH`), 삭제(`DELETE`)는 다음 이슈에서 진행합니다.
- 현재 조회 조건은 `event_start_at`이 구간에 포함되는 일정만 반환합니다. 시작은 구간 밖이지만 종료가 구간에 걸치는 일정(예: 여러 날짜에 걸친 장기 일정)까지 포함하는 진정한 기간-겹침(overlap) 조회는 상세 스펙이 확정되면 반영합니다.

### 검증

- `GetCalendarEventsServiceTest` — 정상 조회 흐름과, `to < from`일 때 리포지토리 호출 없이 예외가 발생하는지 검증했습니다.
- `CalendarControllerTest` — `GET /api/calendars`의 `200`/`400`/`401` 응답 형식을 검증했습니다.
- `./gradlew test`(전체) — calendar 관련 테스트 전부 통과. `attendance` 도메인의 `ApprovalLeaveEventListener` 관련 기존 컨텍스트 로딩 실패 4건은 이번 변경과 무관한 사전 존재 이슈로 확인했습니다(별도 공유).

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
