# 수업 슬롯 API

## 수업 슬롯 등록

### Endpoint

`POST /api/timetables/{timetableSetId}/slots`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- `TIMETABLE:MANAGE` 권한을 보유한 사용자(원장 또는 원장이 권한을 부여한 구성원)만 호출할 수 있다.

### Request Body

```json
{
  "classType": "CLASS",
  "dayOfWeek": "MONDAY",
  "classroomCode": "601",
  "startTime": "09:00",
  "endTime": "11:00",
  "grade": "HIGH_3",
  "teacherName": "정T",
  "subjectName": "미적분",
  "color": "FFCC00"
}
```

| name | type | required | description |
| --- | --- | --- | --- |
| `classType` | Enum | true | `CLASS`/`SPECIAL`/`CLINIC`/`STANDING`/`EXAM` 중 하나 |
| `dayOfWeek` | DayOfWeek | true | 요일 |
| `classroomCode` | String | true | 강의실 코드. 같은 세트 안에서 겹침 검사의 기준이 됨 |
| `startTime` | LocalTime | true | 시작 시각. `endTime`보다 이전이어야 함 |
| `endTime` | LocalTime | true | 종료 시각 |
| `grade` | Enum | true | 학년. `ELEMENTARY_1`~`ELEMENTARY_6`/`MIDDLE_1`~`MIDDLE_3`/`HIGH_1`~`HIGH_3` 중 하나(초1~고3 고정 12단계) |
| `teacherName` | String | false | 강사명 |
| `subjectName` | String | false | 과목 |
| `color` | String | true | 색상(6자리 16진수, RRGGBB) |

`effectiveFrom`/`effectiveUntil`은 요청으로 받지 않으며, 소속 시간표 세트의 `startDate`/`endDate`로 서버가 자동 설정한다.

### Success Response

HTTP `201 Created`

```json
{
  "status": 201,
  "code": "TIMETABLE_201_2",
  "message": "수업 슬롯 등록에 성공했습니다.",
  "data": {
    "timetableSlotId": 1
  }
}
```

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 필수값 누락 등 Bean Validation 위반 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | `TIMETABLE:MANAGE` 권한이 없는 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 시간표 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `409 Conflict` | `TIMETABLE_409_1` | 같은 강의실에 겹치는 시간대의 수업이 이미 있는 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## 수업 슬롯 목록 조회

### Endpoint

`GET /api/timetables/{timetableSetId}/slots`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다. 같은 학원 소속 인증 사용자라면 누구나 호출할 수 있다.

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "TIMETABLE_200_3",
  "message": "수업 슬롯 목록 조회에 성공했습니다.",
  "data": [
    {
      "timetableSlotId": 1,
      "classType": "CLASS",
      "dayOfWeek": "MONDAY",
      "classroomCode": "601",
      "startTime": "09:00:00",
      "endTime": "11:00:00",
      "grade": "HIGH_3",
      "teacherName": "정T",
      "subjectName": "미적분",
      "color": "FFCC00"
    }
  ]
}
```

정렬 순서는 별도로 보장하지 않는다(요일·시간별 그리드 배치는 프론트엔드 책임).

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 시간표 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## 수업 슬롯 상세 조회

### Endpoint

`GET /api/timetables/{timetableSetId}/slots/{timetableSlotId}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다. 같은 학원 소속 인증 사용자라면 누구나 호출할 수 있다.

### Success Response

HTTP `200 OK`

```json
{
  "status": 200,
  "code": "TIMETABLE_200_4",
  "message": "수업 슬롯 상세 조회에 성공했습니다.",
  "data": {
    "timetableSlotId": 1,
    "classType": "CLASS",
    "dayOfWeek": "MONDAY",
    "classroomCode": "601",
    "startTime": "09:00:00",
    "endTime": "11:00:00",
    "grade": "HIGH_3",
    "teacherName": "정T",
    "subjectName": "미적분",
    "color": "FFCC00"
  }
}
```

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 시간표 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `404 Not Found` | `TIMETABLE_404_2` | 슬롯이 존재하지 않거나 다른 세트 소속인 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## 수업 슬롯 수정

### Endpoint

`PATCH /api/timetables/{timetableSetId}/slots/{timetableSlotId}`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- `TIMETABLE:MANAGE` 권한을 보유한 사용자만 호출할 수 있다.

### Request Body

```json
{
  "scope": "ALL",
  "classType": "SPECIAL",
  "dayOfWeek": "TUESDAY",
  "classroomCode": "602",
  "startTime": "13:00",
  "endTime": "15:00",
  "grade": "HIGH_2",
  "teacherName": "오T",
  "subjectName": "물리",
  "color": "00AACC"
}
```

| name | type | required | description |
| --- | --- | --- | --- |
| `scope` | Enum | true | `THIS_OCCURRENCE`/`FROM_NOW`/`ALL` 중 하나. **현재는 `ALL`만 실제로 처리된다.** |
| `classType`/`dayOfWeek`/`classroomCode`/`startTime`/`endTime`/`grade`/`teacherName`/`subjectName`/`color` | - | - | 등록 API와 동일 |

### Success Response

HTTP `204 No Content` (응답 본문 없음)

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 필수값 누락 등 Bean Validation 위반 |
| `400 Bad Request` | `TIMETABLE_400_4` | `scope`가 `ALL`이 아닌 경우(`THIS_OCCURRENCE`/`FROM_NOW`는 아직 지원하지 않음) |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | `TIMETABLE:MANAGE` 권한이 없는 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 시간표 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `404 Not Found` | `TIMETABLE_404_2` | 슬롯이 존재하지 않는 경우 |
| `409 Conflict` | `TIMETABLE_409_1` | 변경하려는 강의실/요일/시간이 다른 슬롯과 겹치는 경우 |
| `409 Conflict` | `TIMETABLE_409_3` | 다른 요청이 먼저 같은 수업 슬롯을 수정한 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |

## 수업 슬롯 삭제

### Endpoint

`DELETE /api/timetables/{timetableSetId}/slots/{timetableSlotId}?scope=ALL`

### 인증 및 권한

- `Authorization: Bearer {accessToken}` 헤더가 필요하다.
- `TIMETABLE:MANAGE` 권한을 보유한 사용자만 호출할 수 있다.

### Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `scope` | Enum | true | `THIS_OCCURRENCE`/`FROM_NOW`/`ALL` 중 하나. **현재는 `ALL`만 실제로 처리된다.** |

### Success Response

HTTP `204 No Content` (응답 본문 없음)

### Error Response

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400 Bad Request` | `TIMETABLE_400_4` | `scope`가 `ALL`이 아닌 경우 |
| `401 Unauthorized` | `COMMON_401_1` | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | `TIMETABLE:MANAGE` 권한이 없는 경우 |
| `404 Not Found` | `TIMETABLE_404_1` | 시간표 세트가 존재하지 않거나 다른 학원 소속인 경우 |
| `404 Not Found` | `TIMETABLE_404_2` | 슬롯이 존재하지 않는 경우 |
| `500 Internal Server Error` | `COMMON_500_1` | 처리되지 않은 서버 오류 |
