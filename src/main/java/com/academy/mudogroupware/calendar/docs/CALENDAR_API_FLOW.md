# 캘린더 API 흐름

## 일정 생성 API 흐름

```text
POST /api/calendars
  → Security Filter
  → AuthUser
  → CalendarController
  → CreateCalendarEventRequest
  → CreateCalendarEventCommand
  → CreateCalendarEventUseCase
  → CreateCalendarEventService
  → CalendarEvent (도메인 팩토리)
  → CalendarEventRepository
  → CalendarEventPersistenceAdapter
  → CalendarEventJpaRepository
  → CreateCalendarEventResponse
  → GlobalApiResponse
```

## 1. 인증 정보 추출

`Security Filter`가 Access Token을 검증하고 `AuthUser`를 만든다. `CalendarController`는 `AuthUser`에서 `userId`를 받아 요청 본문에는 없는 작성자 정보를 결정한다.

작성 API(생성/수정/삭제)는 인증 확인에 더해 `@PreAuthorize("hasAuthority('CALENDAR:MANAGE')")`로 권한도 검사한다(자세한 내용은 [CALENDAR_PERMISSIONS.md](CALENDAR_PERMISSIONS.md) 참고). 조회 API는 인증만 확인하고 권한은 무관하다.

## 2. 요청 검증과 Command 변환

`CreateCalendarEventRequest`는 다음을 Bean Validation으로 검증한다.

- `title`: `@NotBlank`, `@Size(max = 200)`
- `content`: 제약 없음 (nullable)
- `eventStartAt`: `@NotNull`
- `eventEndAt`: 제약 없음 (nullable, 도메인이 시작 시각과 대소 관계를 검증)
- `allDay`: 제약 없음 (기본값 `false`)
- `color`: `@Size(max = 20)` (nullable)

검증을 통과하면 `request.toCommand(authUser)`가 `AuthUser.userId()`를 포함한 `CreateCalendarEventCommand`를 만든다.

## 3. 도메인 팩토리 호출

`CreateCalendarEventService`는 별도의 사전 조회 없이 `CalendarEvent.create(...)`를 호출한다.

- `title`이 공백이면 `CalendarTitleRequiredException`을 던져 `CALENDAR_400_1`로 응답한다.
- `eventEndAt`이 `eventStartAt`보다 이전이면 `InvalidCalendarPeriodException`을 던져 `CALENDAR_400_2`로 응답한다.
- 그 외 필수값(`eventStartAt`, `createdBy`) 누락은 `IllegalArgumentException`으로 방어한다(정상 흐름에서는 Bean Validation과 인증 정보로 이미 채워지므로 도달하지 않음).

이 시점의 `CalendarEvent`는 `id`, `createdAt`, `updatedAt`이 모두 `null`인 신규 도메인 인스턴스이다.

## 4. 영속화

`CalendarEventRepository`(도메인 인터페이스) → `CalendarEventPersistenceAdapter`(어댑터)가 Domain Model을 `CalendarEventEntity`로 변환하고 `CalendarEventJpaRepository.save(...)`를 호출한다.

- `CalendarEventEntity`는 `BaseTimeEntity`를 상속하므로 `created_at`, `updated_at`은 Spring Data JPA Auditing이 저장 시각으로 채운다.
- 저장 후 어댑터가 다시 Domain Model로 변환하며, 이때 DB가 채운 `id`, `createdAt`, `updatedAt`이 포함된다.
- Service는 반환된 Domain Model의 `getId()`를 UseCase 응답으로 사용한다.

## 5. 응답

성공하면 Controller가 `CreateCalendarEventResponse.from(eventId)`로 응답 데이터를 만들고, `GlobalApiResponse.created(CalendarResponseCode.EVENT_CREATED, ...)`로 감싸 HTTP `201 Created`를 반환한다.

## 일정 목록/일별/월간 조회 API 흐름

```text
GET /api/calendars?date= 또는 ?yearMonth=
  → Security Filter
  → AuthUser
  → CalendarController (date/yearMonth → from/to 계산)
  → GetCalendarEventsUseCase
  → GetCalendarEventsService
  → CalendarEventRepository
  → CalendarEventPersistenceAdapter
  → CalendarEventJpaRepository
  → CalendarEventResponse (목록)
  → GlobalApiResponse
```

### 1. 쿼리 파라미터 검증과 구간 계산

`CalendarController`는 `date`(`LocalDate`, 일별)와 `yearMonth`(`YearMonth`, `@DateTimeFormat(pattern = "yyyy-MM")`, 월간)를 둘 다 `@RequestParam(required = false)`로 받는다.

- 형식이 유효하지 않으면 Spring이 자체적으로 `COMMON_400_1`로 응답한다.
- 둘 다 지정했거나 둘 다 지정하지 않으면 `InvalidCalendarQueryException`을 던져 `CALENDAR_400_3`으로 응답한다.
- `date`만 있으면 `from = date.atStartOfDay()`, `to = date.atTime(LocalTime.MAX)`로 그날 하루 구간을 계산한다.
- `yearMonth`만 있으면 `from = yearMonth.atDay(1).atStartOfDay()`, `to = yearMonth.atEndOfMonth().atTime(LocalTime.MAX)`로 그 달 전체 구간을 계산한다.
- 이 계산은 시스템 기본 시간대가 아니라 애플리케이션이 다루는 모든 시각이 이미 한국 시간(Asia/Seoul) 기준이라는 전제(`docs/DATABASE.md`)를 그대로 따른다. 별도의 타임존 변환은 하지 않는다.

### 2. 조회

`GetCalendarEventsService`는 별도 Command 없이 계산된 `from`, `to`를 받아 처리한다(`memo` 도메인의 조회 패턴과 동일하게 Domain Model을 그대로 반환). `CalendarEventRepository.findAllByPeriod(from, to)`를 호출하며 현재 테넌트 DB의 일정만 조회한다.

이 Service는 여전히 `to.isBefore(from)`이면 `InvalidCalendarPeriodException`(`CALENDAR_400_2`)을 던지는 방어 로직을 유지한다. Controller가 항상 유효한 구간을 계산해 넘기므로 이 API 경로에서는 사실상 도달하지 않지만, Service 자체의 불변식으로 남겨둔다.

### 3. 조회 및 변환

`CalendarEventPersistenceAdapter`가 `CalendarEventJpaRepository.findAllByEventStartAtBetween(...)`을 호출해 `event_start_at`이 구간에 포함되는 일정만 가져오고, 각 Entity를 Domain Model로 변환한다.

### 4. 응답

Controller가 반환된 `CalendarEvent` 목록을 `CalendarEventResponse::from`으로 매핑하고, `GlobalApiResponse.ok(CalendarResponseCode.EVENT_LIST_RETRIEVED, ...)`로 감싸 HTTP `200 OK`를 반환한다.

## 일정 수정 API 흐름

```text
PATCH /api/calendars/{eventId}
  → Security Filter
  → AuthUser
  → CalendarController
  → UpdateCalendarEventRequest
  → UpdateCalendarEventCommand
  → UpdateCalendarEventUseCase
  → UpdateCalendarEventService
  → CalendarEvent (도메인, update(...))
  → CalendarEventRepository
  → CalendarEventPersistenceAdapter
  → CalendarEventJpaRepository
  → GlobalApiResponse 없이 204
```

### 1. 요청 검증과 조회

`UpdateCalendarEventRequest`는 `CreateCalendarEventRequest`와 동일한 Bean Validation 제약을 검증한다. 통과하면 `request.toCommand(eventId, authUser)`가 `UpdateCalendarEventCommand`를 만든다.

`UpdateCalendarEventService`는 `CalendarEventRepository.findById(eventId)`로 대상을 조회한다.

- 일정이 존재하지 않으면 `CalendarEventNotFoundException`을 던져 `CALENDAR_404_1`로 응답한다.
- 일정이 현재 테넌트 DB에 없으면 `CalendarEventNotFoundException`(`CALENDAR_404_1`)을 던진다.

### 2. 도메인 검증과 반영

`CalendarEvent.update(...)`가 불변식을 재검증한다.

- `title`이 공백이면 `CalendarTitleRequiredException`을 던져 `CALENDAR_400_1`로 응답한다.
- `eventEndAt`이 `eventStartAt`보다 이전이면 `InvalidCalendarPeriodException`을 던져 `CALENDAR_400_2`로 응답한다.

### 3. 영속화

`CalendarEventRepository.save(event)`를 호출한다. `CalendarEventPersistenceAdapter`는 `id`가 있으면(`updateExisting`) `CalendarEventJpaRepository.getReferenceById(...)`로 관리 상태의 Entity를 가져와 `entity.update(...)`로 필드만 갱신한다. `toEntity`로 새 Entity를 만들어 저장하지 않는 이유는, 새로 만든 Entity는 `createdAt`이 비어 있어 그대로 저장하면 JPA가 기존 `created_at`을 `NULL`로 덮어써 제약 위반이 발생하기 때문이다. `updated_at`은 `BaseTimeEntity`(Spring Data JPA Auditing)가 자동으로 갱신한다.

### 4. 응답

성공하면 Controller가 응답 본문 없이 HTTP `204 No Content`를 반환한다.

## 일정 삭제 API 흐름

```text
DELETE /api/calendars/{eventId}
  → Security Filter
  → AuthUser
  → CalendarController
  → DeleteCalendarEventCommand
  → DeleteCalendarEventUseCase
  → DeleteCalendarEventService
  → CalendarEventRepository
  → CalendarEventPersistenceAdapter
  → CalendarEventJpaRepository
  → GlobalApiResponse 없이 204
```

### 1. 조회 및 소속 학원 검증

`DeleteCalendarEventService`는 `CalendarEventRepository.findById(eventId)`로 대상을 조회한다.

- 일정이 존재하지 않으면 `CalendarEventNotFoundException`을 던져 `CALENDAR_404_1`로 응답한다.
- 일정이 현재 테넌트 DB에 없으면 `CalendarEventNotFoundException`(`CALENDAR_404_1`)을 던진다. 상세조회·수정과 동일한 결정이다.

### 2. 삭제

검증을 통과하면 `CalendarEventRepository.deleteById(eventId)`를 호출한다. 소프트 삭제 플래그 없이 하드 삭제한다.

### 3. 응답

성공하면 Controller가 응답 본문 없이 HTTP `204 No Content`를 반환한다.
