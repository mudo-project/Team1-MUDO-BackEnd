# 근태 API 명세

이 문서는 `role.name`을 응답의 `roleName`으로 반환하는 근태 조회 API를 실제 Controller와 Response DTO 기준으로 정리합니다.

## 전 직원 주간 출결 현황 조회

`GET /api/attendance/employees/weekly`

필요 권한: `ATTENDANCE:READ`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 인증 토큰입니다. |

### Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `date` | `LocalDate` | `true` | 조회할 주에 포함된 날짜입니다. `yyyy-MM-dd` 형식입니다. |
| `keyword` | `String` | `false` | 직원 이름 검색어입니다. |
| `status` | `MyAttendanceDayStatus` | `false` | 해당 상태가 하루 이상 존재하는 직원만 조회합니다. |
| `page` | `int` | `false` | 0부터 시작하며 기본값은 `0`입니다. |
| `size` | `int` | `false` | 1~100이며 기본값은 `20`입니다. |

### Response Body

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_16",
  "message": "주간 전 직원 출결 현황을 조회했습니다.",
  "data": {
    "week": {
      "startDate": "2026-08-10",
      "endDate": "2026-08-16"
    },
    "scheduledWorkDays": 5,
    "employees": {
      "content": [
        {
          "userId": 10,
          "name": "홍길동",
          "roleName": "강사",
          "attendedDays": 1,
          "scheduledWorkDays": 5,
          "days": [
            {
              "date": "2026-08-11",
              "status": "NORMAL",
              "clockInAt": "2026-08-11T09:00:00",
              "clockOutAt": "2026-08-11T18:00:00"
            }
          ]
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "first": true,
      "last": true,
      "hasNext": false,
      "hasPrevious": false
    }
  }
}
```

### Response Field

| name | description |
| --- | --- |
| `data.week.startDate` | 조회 주의 시작일입니다. |
| `data.week.endDate` | 조회 주의 종료일입니다. |
| `data.scheduledWorkDays` | 해당 주의 예정 근무일 수입니다. |
| `data.employees.content[].userId` | 직원 ID입니다. |
| `data.employees.content[].name` | 직원 이름입니다. |
| `data.employees.content[].roleName` | 직원에게 배정된 `role.name`이며, 역할이 없으면 `null`입니다. |
| `data.employees.content[].attendedDays` | 해당 주의 출근일 수입니다. |
| `data.employees.content[].scheduledWorkDays` | 해당 직원에게 표시되는 예정 근무일 수입니다. |
| `data.employees.content[].days[].date` | 근태 날짜입니다. |
| `data.employees.content[].days[].status` | 일별 근태 상태입니다. |
| `data.employees.content[].days[].clockInAt` | 출근 시각이며 기록이 없으면 `null`입니다. |
| `data.employees.content[].days[].clockOutAt` | 퇴근 시각이며 기록이 없으면 `null`입니다. |
| `data.employees.page` | 현재 페이지입니다. |
| `data.employees.size` | 페이지 크기입니다. |
| `data.employees.totalElements` | 조회 조건에 맞는 전체 직원 수입니다. |
| `data.employees.totalPages` | 전체 페이지 수입니다. |
| `data.employees.first` | 첫 페이지 여부입니다. |
| `data.employees.last` | 마지막 페이지 여부입니다. |
| `data.employees.hasNext` | 다음 페이지 존재 여부입니다. |
| `data.employees.hasPrevious` | 이전 페이지 존재 여부입니다. |

### 실패 코드

| HTTP 상태 | code | message | description |
| --- | --- | --- | --- |
| `400 Bad Request` | `ATTENDANCE_400_12` | 조회할 연도와 월이 올바르지 않습니다. | 날짜 또는 페이지 조건이 유효하지 않은 경우입니다. |
| `403 Forbidden` | - | 접근 권한이 없습니다. | `ATTENDANCE:READ` 권한이 없는 경우입니다. |
| `404 Not Found` | `ATTENDANCE_404_1` | 학원의 근무시간 정책을 찾을 수 없습니다. | 현재 근무시간 정책이 없는 경우입니다. |

## 직원 주간 출결 상세 조회

`GET /api/attendance/employees/{userId}/weekly`

필요 권한: `ATTENDANCE:READ`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 인증 토큰입니다. |

### Request Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `userId` | `Long` | `true` | 조회할 직원 ID입니다. |

### Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `date` | `LocalDate` | `true` | 조회할 주에 포함된 날짜입니다. `yyyy-MM-dd` 형식입니다. |

### Response Body

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_17",
  "message": "직원의 주간 출결 상세를 조회했습니다.",
  "data": {
    "employee": {
      "userId": 10,
      "name": "홍길동",
      "roleName": "강사"
    },
    "week": {
      "startDate": "2026-08-10",
      "endDate": "2026-08-16"
    },
    "days": [
      {
        "date": "2026-08-11",
        "status": "NORMAL",
        "clockInAt": "2026-08-11T09:00:00",
        "clockOutAt": "2026-08-11T18:00:00"
      }
    ],
    "weeklySummary": {
      "scheduledWorkDays": 5,
      "attendedDays": 1
    }
  }
}
```

### Response Field

| name | description |
| --- | --- |
| `data.employee.userId` | 직원 ID입니다. |
| `data.employee.name` | 직원 이름입니다. |
| `data.employee.roleName` | 직원에게 배정된 `role.name`입니다. 역할이 없으면 `null`입니다. |
| `data.week.startDate` | 조회 주의 시작일입니다. |
| `data.week.endDate` | 조회 주의 종료일입니다. |
| `data.days[].date` | 근태 날짜입니다. |
| `data.days[].status` | 일별 근태 상태입니다. |
| `data.days[].clockInAt` | 출근 시각이며 기록이 없으면 `null`입니다. |
| `data.days[].clockOutAt` | 퇴근 시각이며 기록이 없으면 `null`입니다. |
| `data.weeklySummary.scheduledWorkDays` | 해당 주의 예정 근무일 수입니다. |
| `data.weeklySummary.attendedDays` | 해당 주의 출근일 수입니다. |

### 실패 코드

| HTTP 상태 | code | message | description |
| --- | --- | --- | --- |
| `400 Bad Request` | `ATTENDANCE_400_12` | 조회할 연도와 월이 올바르지 않습니다. | 조회 기준 날짜가 유효하지 않은 경우입니다. |
| `403 Forbidden` | - | 접근 권한이 없습니다. | `ATTENDANCE:READ` 권한이 없는 경우입니다. |
| `404 Not Found` | `ATTENDANCE_404_1` | 학원의 근무시간 정책을 찾을 수 없습니다. | 현재 근무시간 정책이 없는 경우입니다. |
| `404 Not Found` | `ATTENDANCE_404_5` | 조회할 직원을 찾을 수 없습니다. | 조회 대상 직원을 찾을 수 없는 경우입니다. |

## 관리자 근태 수정 요청 목록 조회

`GET /api/attendance/correction-requests`

필요 권한: `ATTENDANCE:CORRECTION_READ`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 인증 토큰입니다. |

### Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `status` | `AttendanceCorrectionStatus` | `false` | `PENDING`, `APPROVED`, `REJECTED` 중 하나입니다. |
| `page` | `int` | `false` | 0부터 시작하며 기본값은 `0`입니다. |
| `size` | `int` | `false` | 1~100이며 기본값은 `30`입니다. |

### Response Body

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_12",
  "message": "근태 수정 요청 목록이 조회되었습니다.",
  "data": {
    "content": [
      {
        "requestId": 1,
        "requester": {
          "userId": 10,
          "name": "홍길동",
          "roleName": "강사"
        },
        "workDate": "2026-08-11",
        "type": "CLOCK_OUT_TIME",
        "status": "PENDING",
        "originalClockInAt": "2026-08-11T09:00:00",
        "originalClockOutAt": null,
        "originalClockInNote": null,
        "originalClockOutNote": null,
        "requestedClockInAt": null,
        "requestedClockOutAt": "2026-08-11T18:00:00",
        "requestedClockInNote": null,
        "requestedClockOutNote": null,
        "reason": "퇴근 처리를 누락했습니다.",
        "requestedAt": "2026-08-11T19:00:00",
        "processedAt": null,
        "processedBy": null,
        "rejectionReason": null
      }
    ],
    "page": 0,
    "size": 30,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

### Response Field

| name | description |
| --- | --- |
| `data.content[].requester.userId` | 요청자 ID입니다. |
| `data.content[].requester.name` | 요청자 이름입니다. |
| `data.content[].requester.roleName` | 요청자에게 배정된 `role.name`입니다. 역할이 없으면 `null`입니다. |
| `data.content[].requestId` | 근태 수정 요청 ID입니다. |
| `data.content[].workDate` | 수정 대상 근무일입니다. |
| `data.content[].type` | 수정 요청 유형입니다. |
| `data.content[].status` | 처리 상태입니다. |
| `data.content[].originalClockInAt` | 기존 출근 시각입니다. |
| `data.content[].originalClockOutAt` | 기존 퇴근 시각입니다. |
| `data.content[].requestedClockInAt` | 요청한 출근 시각입니다. |
| `data.content[].requestedClockOutAt` | 요청한 퇴근 시각입니다. |
| `data.content[].reason` | 수정 요청 사유입니다. |
| `data.content[].requestedAt` | 요청 시각입니다. |
| `data.content[].processedAt` | 처리 시각이며 미처리 상태이면 `null`입니다. |
| `data.content[].processedBy` | 처리자 ID이며 미처리 상태이면 `null`입니다. |
| `data.content[].rejectionReason` | 반려 사유이며 반려되지 않았으면 `null`입니다. |
| `data.page` | 현재 페이지입니다. |
| `data.size` | 페이지 크기입니다. |
| `data.totalElements` | 조회 조건에 맞는 전체 근태 수정 요청 수입니다. |
| `data.totalPages` | 전체 페이지 수입니다. |
| `data.first` | 첫 페이지 여부입니다. |
| `data.last` | 마지막 페이지 여부입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |
| `data.hasPrevious` | 이전 페이지 존재 여부입니다. |

### 실패 코드

| HTTP 상태 | code | message | description |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 유효하지 않은 요청입니다. | 상태값 또는 페이지 조건이 유효하지 않은 경우입니다. |
| `403 Forbidden` | - | 접근 권한이 없습니다. | `ATTENDANCE:CORRECTION_READ` 권한이 없는 경우입니다. |

## 관리자 근태 수정 요청 상세 조회

`GET /api/attendance/correction-requests/{requestId}`

필요 권한: `ATTENDANCE:CORRECTION_READ`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 인증 토큰입니다. |

### Request Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `requestId` | `Long` | `true` | 조회할 근태 수정 요청 ID입니다. |

### Response Body

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_13",
  "message": "근태 수정 요청이 조회되었습니다.",
  "data": {
    "requestId": 1,
    "requester": {
      "userId": 10,
      "name": "홍길동",
      "roleName": "강사"
    },
    "workDate": "2026-08-11",
    "type": "CLOCK_OUT_TIME",
    "status": "PENDING",
    "originalClockInAt": "2026-08-11T09:00:00",
    "originalClockOutAt": null,
    "originalClockInNote": null,
    "originalClockOutNote": null,
    "requestedClockInAt": null,
    "requestedClockOutAt": "2026-08-11T18:00:00",
    "requestedClockInNote": null,
    "requestedClockOutNote": null,
    "reason": "퇴근 처리를 누락했습니다.",
    "requestedAt": "2026-08-11T19:00:00",
    "processedAt": null,
    "processedBy": null,
    "rejectionReason": null
  }
}
```

상세 응답의 필드는 목록 응답의 `data.content[]`와 동일합니다. `data.requester.roleName`은 요청자에게 배정된 `role.name`이며 역할이 없으면 `null`입니다.

### 실패 코드

| HTTP 상태 | code | message | description |
| --- | --- | --- | --- |
| `403 Forbidden` | - | 접근 권한이 없습니다. | `ATTENDANCE:CORRECTION_READ` 권한이 없는 경우입니다. |
| `404 Not Found` | `ATTENDANCE_404_4` | 근태 수정 요청을 찾을 수 없습니다. | 요청 ID에 해당하는 수정 요청이 없는 경우입니다. |

## 관리자 근태 수정 요청 승인

`POST /api/attendance/correction-requests/{requestId}/approve`

필요 권한: `ATTENDANCE:CORRECTION_PROCESS`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 인증 토큰입니다. |

### Request Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `requestId` | `Long` | `true` | 승인할 근태 수정 요청 ID입니다. |

Request Body는 없습니다.

### Response Body

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_14",
  "message": "근태 수정 요청이 승인되었습니다.",
  "data": null
}
```

### Response Field

| name | description |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | `ATTENDANCE_200_14`입니다. |
| `message` | 승인 완료 메시지입니다. |
| `data` | 별도의 처리 결과 객체를 반환하지 않으므로 `null`입니다. |

### 실패 코드

| HTTP 상태 | code | message | description |
| --- | --- | --- | --- |
| `400 Bad Request` | `ATTENDANCE_400_12` | 근태 수정 요청값이 올바르지 않습니다. | 요청 내용으로 근태 기록을 수정할 수 없는 경우입니다. |
| `403 Forbidden` | - | 접근 권한이 없습니다. | `ATTENDANCE:CORRECTION_PROCESS` 권한이 없는 경우입니다. |
| `404 Not Found` | `ATTENDANCE_404_1` | 학원의 근무시간 정책을 찾을 수 없습니다. | 출근 시각을 반영하는 데 필요한 근무시간 정책이 없는 경우입니다. |
| `404 Not Found` | `ATTENDANCE_404_3` | 수정할 근태 기록을 찾을 수 없습니다. | 기존 기록이 필요한 수정 유형이지만 근태 기록이 없는 경우입니다. |
| `404 Not Found` | `ATTENDANCE_404_4` | 근태 수정 요청을 찾을 수 없습니다. | 요청 ID에 해당하는 수정 요청이 없는 경우입니다. |
| `409 Conflict` | `ATTENDANCE_409_2` | 오늘은 근무일이 아닙니다. | 출근 시각을 반영할 날짜가 근무일이 아닌 경우입니다. |
| `409 Conflict` | `ATTENDANCE_409_8` | 이미 처리된 근태 수정 요청입니다. | 이미 승인 또는 반려된 요청을 다시 처리하는 경우입니다. |

## 관리자 근태 수정 요청 반려

`POST /api/attendance/correction-requests/{requestId}/reject`

필요 권한: `ATTENDANCE:CORRECTION_PROCESS`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 인증 토큰입니다. |

### Request Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `requestId` | `Long` | `true` | 반려할 근태 수정 요청 ID입니다. |

### Request Body

```json
{
  "reason": "증빙이 부족합니다."
}
```

| name | type | required | description |
| --- | --- | --- | --- |
| `reason` | `String` | `true` | 반려 사유입니다. 공백일 수 없으며 최대 500자입니다. |

### Response Body

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_15",
  "message": "근태 수정 요청이 반려되었습니다.",
  "data": null
}
```

### Response Field

| name | description |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | `ATTENDANCE_200_15`입니다. |
| `message` | 반려 완료 메시지입니다. |
| `data` | 별도의 처리 결과 객체를 반환하지 않으므로 `null`입니다. |

### 실패 코드

| HTTP 상태 | code | message | description |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 유효하지 않은 요청입니다. | 반려 사유가 공백이거나 500자를 초과한 경우입니다. |
| `403 Forbidden` | - | 접근 권한이 없습니다. | `ATTENDANCE:CORRECTION_PROCESS` 권한이 없는 경우입니다. |
| `404 Not Found` | `ATTENDANCE_404_4` | 근태 수정 요청을 찾을 수 없습니다. | 요청 ID에 해당하는 수정 요청이 없는 경우입니다. |
| `409 Conflict` | `ATTENDANCE_409_8` | 이미 처리된 근태 수정 요청입니다. | 이미 승인 또는 반려된 요청을 다시 처리하는 경우입니다. |
