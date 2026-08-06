# 캘린더 API

## 일정 생성

### Endpoint

`POST /api/calendars`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 현재 구현은 인증된 사용자라면 호출할 수 있다.
- 기능명세서상 "대표와 대표가 허용한 권한"은 `users.role` 값 체계 확정 후 `@PreAuthorize`로 적용 예정이며, 지금은 `CalendarController`에 TODO로 남긴다.

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` 형식의 Access Token |
| `Content-Type` | `application/json` |

### Request Body

```json
{
  "title": "2학기 수업 준비 회의",
  "content": "2학기 수업 계획 논의 및 교재 배분",
  "eventStartAt": "2026-08-03T10:00:00",
  "eventEndAt": "2026-08-03T11:30:00",
  "allDay": false,
  "color": "green"
}
```

| name | type | required | description |
| --- | --- | --- | --- |
| `title` | String | true | 일정 제목. 공백만 입력할 수 없고 최대 200자 |
| `content` | String | false | 일정 내용 |
| `eventStartAt` | LocalDateTime | true | 일정 시작 일시 |
| `eventEndAt` | LocalDateTime | false | 일정 종료 일시. 값이 있으면 `eventStartAt` 이후여야 함 |
| `allDay` | boolean | false | 종일 일정 여부. 생략 시 `false` |
| `color` | String | false | 표시 색상 코드. 최대 20자 |

### Success Response

HTTP `201 Created`

```json
{
  "status": 201,
  "code": "CALENDAR_201_1",
  "message": "일정 생성에 성공했습니다.",
  "data": {
    "eventId": 1
  }
}
```

| name | description |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 응답 코드 (`CALENDAR_201_1`) |
| `message` | 응답 메시지 |
| `data.eventId` | 생성된 일정 번호 |

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | `title` 누락·공백·200자 초과, `eventStartAt` 누락, `color` 20자 초과 등 Bean Validation 위반 |
| `400 Bad Request` | `CALENDAR_400_1` | 도메인 검증에서 `title`이 공백으로 판정된 경우 |
| `400 Bad Request` | `CALENDAR_400_2` | `eventEndAt`이 `eventStartAt`보다 이전인 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- `academyId`와 `createdBy`는 요청 본문이 아니라 Access Token의 인증 정보(`AuthUser`)에서 가져온다.
- `createdAt`, `updatedAt`은 `BaseTimeEntity`(Spring Data JPA Auditing)가 저장 시 자동으로 채운다.
- 도메인 검증은 `CalendarEvent.create(...)` 내부에서 수행하며, 위반 시 `CalendarTitleRequiredException`(`CALENDAR_400_1`) 또는 `InvalidCalendarPeriodException`(`CALENDAR_400_2`)이 발생한다.
- 자세한 처리 흐름은 [CALENDAR_API_FLOW.md](CALENDAR_API_FLOW.md), 도메인 규칙은 [BUSINESS_RULES.md](BUSINESS_RULES.md)를 참고한다.
