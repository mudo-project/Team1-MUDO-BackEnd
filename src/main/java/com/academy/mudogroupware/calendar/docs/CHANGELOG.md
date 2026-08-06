# 📚 Calendar Changelog

## 2026-08-06 · 일정 수정 API 추가 ✨

- `PATCH /api/calendars/{eventId}`로 학원 공용 캘린더 일정을 수정할 수 있습니다.
- 요청 필드는 생성 API와 동일하며, 부분 필드가 아니라 수정 가능한 필드 전체를 매번 새 값으로 교체합니다.
- 일정이 존재하지 않거나 다른 학원 소속이면 `CALENDAR_404_1`로 응답합니다.
- `title`이 공백이거나 `eventEndAt`이 `eventStartAt`보다 이전이면 각각 `CALENDAR_400_1`, `CALENDAR_400_2`로 응답합니다.
- 성공 시 응답 본문 없이 HTTP `204 No Content`를 반환합니다.
- `updated_at`은 `BaseTimeEntity`(Spring Data JPA Auditing)가 자동으로 갱신합니다.
## 2026-08-06 · 일정 상세 조회 API 추가 ✨

- `GET /api/calendars/{eventId}`로 일정 상세를 조회할 수 있습니다.
- 일정이 존재하지 않거나 다른 학원 소속이면 `CALENDAR_404_1`로 응답합니다(다른 학원에 존재 여부를 노출하지 않기 위해 두 경우를 구분하지 않음).
- 성공 시 HTTP `200 OK`와 함께 `CalendarEventResponse`(목록조회와 동일한 응답 형태)를 반환합니다.

## 2026-08-06 · 일정 목록/일별 조회 API 추가 ✨

- `GET /api/calendars?from=&to=`로 학원 공용 캘린더 일정을 기간 조회할 수 있습니다. 목록조회와 일별조회를 겸용합니다.
- 조회 대상은 요청자의 `academyId` 소속 일정으로 한정합니다.
- `to`가 `from`보다 이전이면 `CALENDAR_400_2`로 응답합니다.
- 성공 시 HTTP `200 OK`와 함께 일정 목록(`CalendarEventResponse[]`)을 반환합니다.
- `CalendarEventResponse`는 목록/일별/상세 조회에서 공용으로 재사용합니다.

## 2026-08-06 · 캘린더 도메인 신설 및 일정 생성 API 추가 ✨

- 학원 공용 캘린더를 담당하는 `calendar` top-level 도메인 모듈을 신설했습니다.
- `POST /api/calendars`로 학원 공용 캘린더에 일정을 등록할 수 있습니다.
- 요청 본문에는 제목·내용·시작/종료 일시·종일 여부·색상을 담고, 학원 번호와 작성자는 Access Token의 인증 정보로 채웁니다.
- `title`이 공백이거나 `eventEndAt`이 `eventStartAt`보다 이전이면 각각 `CALENDAR_400_1`, `CALENDAR_400_2`로 응답합니다.
- 성공 시 HTTP `201 Created`와 함께 생성된 일정 번호(`eventId`)를 반환합니다.
- 도메인 규칙 위반은 `CalendarErrorCode` + 에러별 이름이 드러나는 개별 예외 클래스로 표현합니다(`docs/ERROR_HANDLING.md` 표준 패턴, `workspace`와 동일한 방식).
- 영속성 구현체 이름은 헥사고날 용어를 충실히 따라 `CalendarEventPersistenceAdapter`를 사용합니다.
- `created_at`, `updated_at`은 `BaseTimeEntity`(Spring Data JPA Auditing)로 자동 관리합니다.
- `be5/V5.1.1__create_calendar_events_table.sql` 마이그레이션과 도메인/서비스/컨트롤러 단위 테스트를 추가했습니다.
- 작성 권한(대표 및 대표가 허용한 권한) 실제 검사는 `users.role` 값 체계가 확정된 뒤 반영하며, 현재는 `CalendarController`에 TODO 주석으로만 남깁니다(`notice` 도메인과 동일한 결정).

자세한 요청·응답 형식은 [CALENDAR_API.md](CALENDAR_API.md), 처리 흐름은 [CALENDAR_API_FLOW.md](CALENDAR_API_FLOW.md), 정책은 [BUSINESS_RULES.md](BUSINESS_RULES.md)를 참고해주세요. 📚
