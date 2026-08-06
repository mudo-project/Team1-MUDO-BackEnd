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

## 일정 목록/일별 조회

### Endpoint

`GET /api/calendars?from={from}&to={to}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 같은 학원(`AuthUser.academyId()`) 소속으로 인증된 사용자라면 누구나 호출할 수 있다. 별도 권한 검사는 없다.

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` 형식의 Access Token |

### Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `from` | LocalDateTime | true | 조회 시작 일시. 예: `2026-08-01T00:00:00` |
| `to` | LocalDateTime | true | 조회 종료 일시. `from`보다 이전일 수 없음. 예: `2026-08-31T23:59:59` |

일별 조회는 같은 날의 `00:00:00`~`23:59:59`를 `from`/`to`에 각각 넣어서 호출한다.

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "CALENDAR_200_1",
  "message": "일정 목록 조회에 성공했습니다.",
  "data": [
    {
      "eventId": 1,
      "title": "2학기 수업 준비 회의",
      "content": "2학기 수업 계획 논의 및 교재 배분",
      "eventStartAt": "2026-08-03T10:00:00",
      "eventEndAt": "2026-08-03T11:30:00",
      "allDay": false,
      "color": "green",
      "createdBy": 7,
      "createdAt": "2026-08-03T09:00:00",
      "updatedAt": "2026-08-03T09:00:00"
    }
  ]
}
```

| name | description |
| --- | --- |
| `data[].eventId` | 일정 번호 |
| `data[].title` | 일정 제목 |
| `data[].content` | 일정 내용 |
| `data[].eventStartAt` | 일정 시작 일시 |
| `data[].eventEndAt` | 일정 종료 일시 |
| `data[].allDay` | 종일 일정 여부 |
| `data[].color` | 표시 색상 코드 |
| `data[].createdBy` | 작성자 사용자 번호 |
| `data[].createdAt` | 생성 일시 |
| `data[].updatedAt` | 수정 일시 |

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | `from`/`to` 누락 또는 형식이 유효하지 않음 |
| `400 Bad Request` | `CALENDAR_400_2` | `to`가 `from`보다 이전인 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- 조회 대상은 요청자의 `academyId` 소속 일정으로 한정한다. 다른 학원의 일정은 조회되지 않는다.
- 현재 조회 조건은 `event_start_at`이 `[from, to]` 구간에 포함되는 일정만 반환한다(종료 시각이 구간 밖까지 걸치는 일정은 포함하지 않음).
- 목록/일별/상세 조회는 모두 `CalendarEventResponse`를 공용으로 사용한다.

## 일정 수정

### Endpoint

`PATCH /api/calendars/{eventId}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 현재 구현은 인증된 사용자라면 호출할 수 있다.
- 기능명세서상 "대표와 대표가 허용한 권한"은 `users.role` 값 체계 확정 후 `@PreAuthorize`로 적용 예정이며, 지금은 `CalendarController`에 TODO로 남긴다.

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` 형식의 Access Token |
| `Content-Type` | `application/json` |

### Path Variable

| name | type | required | description |
| --- | --- | --- | --- |
| `eventId` | Long | true | 수정할 일정 번호 |

### Request Body

```json
{
  "title": "2학기 수업 준비 회의 (변경)",
  "content": "회의실 변경 안내 추가",
  "eventStartAt": "2026-08-04T12:30:00",
  "eventEndAt": "2026-08-04T15:30:00",
  "allDay": true,
  "color": "orange"
}
```

생성 API와 동일한 필드 구성이다. 부분 필드만 보내는 방식(PATCH의 일반적 의미)이 아니라, 수정 가능한 필드 전체를 매번 새 값으로 통째로 교체한다.

| name | type | required | description |
| --- | --- | --- | --- |
| `title` | String | true | 일정 제목. 공백만 입력할 수 없고 최대 200자 |
| `content` | String | false | 일정 내용 |
| `eventStartAt` | LocalDateTime | true | 일정 시작 일시 |
| `eventEndAt` | LocalDateTime | false | 일정 종료 일시. 값이 있으면 `eventStartAt` 이후여야 함 |
| `allDay` | boolean | false | 종일 일정 여부. 생략 시 `false` |
| `color` | String | false | 표시 색상 코드. 최대 20자 |

### Success Response

HTTP `204 No Content` (응답 본문 없음)

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | `title` 누락·공백·200자 초과, `eventStartAt` 누락, `color` 20자 초과 등 Bean Validation 위반 |
| `400 Bad Request` | `CALENDAR_400_1` | 도메인 검증에서 `title`이 공백으로 판정된 경우 |
| `400 Bad Request` | `CALENDAR_400_2` | `eventEndAt`이 `eventStartAt`보다 이전인 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `CALENDAR_404_1` | 일정이 존재하지 않거나 다른 학원 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- 다른 학원 소속 일정을 수정하려고 하면 존재 여부를 노출하지 않기 위해 "존재하지 않음"과 동일하게 `CALENDAR_404_1`로 응답한다(별도의 403 응답을 두지 않음).
- `academyId`, `createdBy`, `createdAt`은 수정 대상이 아니다. `updatedAt`은 `BaseTimeEntity`(Spring Data JPA Auditing)가 수정 시 자동으로 갱신한다.
- 도메인 검증은 `CalendarEvent.update(...)` 내부에서 수행하며, 위반 시 `CalendarTitleRequiredException`(`CALENDAR_400_1`) 또는 `InvalidCalendarPeriodException`(`CALENDAR_400_2`)이 발생한다.

## 일정 상세 조회

### Endpoint

`GET /api/calendars/{eventId}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- 같은 학원(`AuthUser.academyId()`) 소속으로 인증된 사용자라면 누구나 호출할 수 있다. 별도 권한 검사는 없다.

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` 형식의 Access Token |

### Path Variable

| name | type | required | description |
| --- | --- | --- | --- |
| `eventId` | Long | true | 조회할 일정 번호 |

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "CALENDAR_200_2",
  "message": "일정 상세 조회에 성공했습니다.",
  "data": {
    "eventId": 1,
    "title": "2학기 수업 준비 회의",
    "content": "2학기 수업 계획 논의 및 교재 배분",
    "eventStartAt": "2026-08-03T10:00:00",
    "eventEndAt": "2026-08-03T11:30:00",
    "allDay": false,
    "color": "green",
    "createdBy": 7,
    "createdAt": "2026-08-03T09:00:00",
    "updatedAt": "2026-08-03T09:00:00"
  }
}
```

응답 필드는 목록조회와 동일한 `CalendarEventResponse`를 사용한다.

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `CALENDAR_404_1` | 일정이 존재하지 않거나 다른 학원 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- 다른 학원 소속 일정을 조회하면 존재 여부를 노출하지 않기 위해 "존재하지 않음"과 동일하게 `CALENDAR_404_1`로 응답한다(별도의 403 응답을 두지 않음).
