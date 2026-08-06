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

`Security Filter`가 Access Token을 검증하고 `AuthUser`를 만든다. `CalendarController`는 `AuthUser`에서 `academyId`와 `userId`를 받아 요청 본문에는 없는 학원·작성자 정보를 결정한다.

권한 모듈 연동 전이므로 대표 권한 검사는 수행하지 않는다. `notice`와 동일하게 인증만 검사하며, `CalendarController`에 TODO를 남긴다.

## 2. 요청 검증과 Command 변환

`CreateCalendarEventRequest`는 다음을 Bean Validation으로 검증한다.

- `title`: `@NotBlank`, `@Size(max = 200)`
- `content`: 제약 없음 (nullable)
- `eventStartAt`: `@NotNull`
- `eventEndAt`: 제약 없음 (nullable, 도메인이 시작 시각과 대소 관계를 검증)
- `allDay`: 제약 없음 (기본값 `false`)
- `color`: `@Size(max = 20)` (nullable)

검증을 통과하면 `request.toCommand(authUser)`가 `AuthUser.academyId()`와 `AuthUser.userId()`를 포함한 `CreateCalendarEventCommand`를 만든다.

## 3. 도메인 팩토리 호출

`CreateCalendarEventService`는 별도의 사전 조회 없이 `CalendarEvent.create(...)`를 호출한다.

- `title`이 공백이면 `CalendarTitleRequiredException`을 던져 `CALENDAR_400_1`로 응답한다.
- `eventEndAt`이 `eventStartAt`보다 이전이면 `InvalidCalendarPeriodException`을 던져 `CALENDAR_400_2`로 응답한다.
- 그 외 필수값(`academyId`, `eventStartAt`, `createdBy`) 누락은 `IllegalArgumentException`으로 방어한다(정상 흐름에서는 Bean Validation과 인증 정보로 이미 채워지므로 도달하지 않음).

이 시점의 `CalendarEvent`는 `id`, `createdAt`, `updatedAt`이 모두 `null`인 신규 도메인 인스턴스이다.

## 4. 영속화

`CalendarEventRepository`(도메인 인터페이스) → `CalendarEventPersistenceAdapter`(어댑터)가 Domain Model을 `CalendarEventEntity`로 변환하고 `CalendarEventJpaRepository.save(...)`를 호출한다.

- `CalendarEventEntity`는 `BaseTimeEntity`를 상속하므로 `created_at`, `updated_at`은 Spring Data JPA Auditing이 저장 시각으로 채운다.
- 저장 후 어댑터가 다시 Domain Model로 변환하며, 이때 DB가 채운 `id`, `createdAt`, `updatedAt`이 포함된다.
- Service는 반환된 Domain Model의 `getId()`를 UseCase 응답으로 사용한다.

## 5. 응답

성공하면 Controller가 `CreateCalendarEventResponse.from(eventId)`로 응답 데이터를 만들고, `GlobalApiResponse.created(CalendarResponseCode.EVENT_CREATED, ...)`로 감싸 HTTP `201 Created`를 반환한다.
