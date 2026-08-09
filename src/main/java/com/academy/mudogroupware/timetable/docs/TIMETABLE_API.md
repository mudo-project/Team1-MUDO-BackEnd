# 시간표 세트 API

## 시간표 세트 생성

### Endpoint

`POST /api/timetables`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- `TIMETABLE:MANAGE` 권한을 보유한 사용자(원장 또는 원장이 권한을 부여한 구성원)만 호출할 수 있다.

### Request Body

```json
{
  "name": "2026 여름특강",
  "startDate": "2026-07-20",
  "endDate": "2026-08-16",
  "operatingStartTime": "08:30",
  "operatingEndTime": "22:00",
  "operatingDays": ["MONDAY", "WEDNESDAY"],
  "slotUnitMinutes": 30,
  "classrooms": [
    {"floor": "6층", "codes": ["601", "602", "603", "604", "605"]},
    {"floor": "5층", "codes": ["501", "502"]}
  ]
}
```

| name | type | required | description |
| --- | --- | --- | --- |
| `name` | String | true | 시간표 세트 이름. 같은 학원 내 유일해야 함 |
| `startDate` | LocalDate | true | 시작일 |
| `endDate` | LocalDate | true | 종료일. `startDate` 이후여야 함 |
| `operatingStartTime` | LocalTime | true | 운영 시작 시각 |
| `operatingEndTime` | LocalTime | true | 운영 종료 시각 |
| `operatingDays` | DayOfWeek[] | true | 운영 요일. 최소 1개 |
| `slotUnitMinutes` | int | true | 슬롯 단위(분). 양수 |
| `classrooms` | ClassroomGroup[] | true | 층별 강의실 구성. `code`는 세트 내 유일해야 함 |

### Success Response

HTTP `201 Created`

```json
{
  "status": 201,
  "code": "TIMETABLE_201_1",
  "message": "시간표 세트 생성에 성공했습니다.",
  "data": {
    "timetableSetId": 1
  }
}
```

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 필수값 누락 등 Bean Validation 위반 |
| `400 Bad Request` | `TIMETABLE_400_1` | 도메인 검증에서 `name`이 공백으로 판정된 경우 |
| `400 Bad Request` | `TIMETABLE_400_2` | `endDate`가 `startDate`보다 이전인 경우 |
| `400 Bad Request` | `TIMETABLE_400_3` | 강의실 `code`가 세트 내에서 중복된 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | `TIMETABLE:MANAGE` 권한이 없는 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## 시간표 세트 목록 조회

### Endpoint

`GET /api/timetables`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다. 같은 학원 소속 인증 사용자라면 누구나 호출할 수 있다.

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "TIMETABLE_200_1",
  "message": "시간표 세트 목록 조회에 성공했습니다.",
  "data": [
    {
      "timetableSetId": 1,
      "name": "2026 여름특강",
      "startDate": "2026-07-20",
      "endDate": "2026-08-16",
      "status": "ACTIVE"
    }
  ]
}
```

시작일(`startDate`) 최신순으로 정렬된다. `status`는 `PLANNED`/`ACTIVE`/`ENDED` 중 하나다.

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## 시간표 세트 상세 조회

### Endpoint

`GET /api/timetables/{timetableSetId}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다. 같은 학원 소속 인증 사용자라면 누구나 호출할 수 있다.

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "TIMETABLE_200_2",
  "message": "시간표 세트 상세 조회에 성공했습니다.",
  "data": {
    "timetableSetId": 1,
    "name": "2026 여름특강",
    "startDate": "2026-07-20",
    "endDate": "2026-08-16",
    "operatingStartTime": "08:30:00",
    "operatingEndTime": "22:00:00",
    "operatingDays": ["MONDAY", "WEDNESDAY"],
    "slotUnitMinutes": 30,
    "classrooms": [
      {"floor": "6층", "codes": ["601", "602"]}
    ],
    "status": "ACTIVE"
  }
}
```

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- 다른 학원 소속 세트를 조회하려고 하면 존재 여부를 노출하지 않기 위해 "존재하지 않음"과 동일하게 `TIMETABLE_404_1`로 응답한다(별도의 403 응답을 두지 않음).

## 시간표 세트 수정

### Endpoint

`PATCH /api/timetables/{timetableSetId}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- `TIMETABLE:MANAGE` 권한을 보유한 사용자만 호출할 수 있다.

### Request Body

생성 API와 동일한 필드 구성이다. 부분 필드만 보내는 방식이 아니라, 수정 가능한 필드 전체를 매번 새 값으로 통째로 교체한다.

### Success Response

HTTP `204 No Content` (응답 본문 없음)

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 필수값 누락 등 Bean Validation 위반 |
| `400 Bad Request` | `TIMETABLE_400_1` | 도메인 검증에서 `name`이 공백으로 판정된 경우 |
| `400 Bad Request` | `TIMETABLE_400_2` | `endDate`가 `startDate`보다 이전인 경우 |
| `400 Bad Request` | `TIMETABLE_400_3` | 강의실 `code`가 세트 내에서 중복된 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | `TIMETABLE:MANAGE` 권한이 없는 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## 시간표 세트 삭제

### Endpoint

`DELETE /api/timetables/{timetableSetId}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- `TIMETABLE:MANAGE` 권한을 보유한 사용자만 호출할 수 있다.

### Success Response

HTTP `204 No Content` (응답 본문 없음)

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | `TIMETABLE:MANAGE` 권한이 없는 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

### Business Rules

- 세트를 삭제하면 그 안의 강의실 구성(`timetable_set_classroom`)도 `ON DELETE CASCADE`로 함께 삭제된다.

## 시간표 세트 내보내기

`GET /api/timetables/{timetableSetId}/export` (엑셀/PDF/PNG) — [TIMETABLE_EXPORT_API.md](TIMETABLE_EXPORT_API.md) 참고.
