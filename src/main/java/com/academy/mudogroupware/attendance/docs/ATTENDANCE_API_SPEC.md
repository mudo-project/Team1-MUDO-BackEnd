# 근태 API 명세

> 기준일: 2026-08-14
> 기준: Notion `API 명세서` 데이터베이스의 `EPIC = 근태` 항목과 현재 Controller, Request/Response DTO, Security, 성공·오류 코드 구현
> 충돌 시 현재 코드 계약을 우선합니다.

## 공통 규칙

### 인증과 응답 형식

- 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요합니다.
- `GET /api/attendance/wifi-ips/current`는 메서드 수준 권한 어노테이션이 없지만, 전역 Security 설정의 `anyRequest().authenticated()` 적용 대상입니다.
- 별도 권한이 적혀 있지 않은 API는 인증된 사용자라면 호출할 수 있습니다.
- 성공 응답은 `GlobalApiResponse`를 사용합니다.

### Next 서버의 클라이언트 IP 전달

- 운영 환경에서 현재 IP 조회, Wi-Fi IP 등록, 출근, 퇴근 요청은 Next 서버가 백엔드로 전달합니다.
- Next 서버는 신뢰할 수 있는 앞단 프록시에서 확인한 IP를 아래 헤더로 전달해야 합니다.
- 브라우저가 보낸 동일 이름의 헤더는 제거하고 Next 서버가 값을 새로 설정해야 합니다.

| 헤더 | 값 |
| --- | --- |
| `X-Client-IP` | 검증한 IPv4 또는 IPv6 주소 |
| `X-Client-IP-Timestamp` | 요청 서명 시각의 Unix epoch seconds |
| `X-Client-IP-Signature` | 아래 payload의 HMAC-SHA256 서명을 Base64 URL-safe, padding 없이 인코딩한 값 |

서명 payload는 다음 네 값을 줄바꿈 문자(`\n`)로 연결합니다. 경로에는 query string을 포함하지 않습니다.

```text
{HTTP_METHOD}\n{REQUEST_PATH}\n{CLIENT_IP}\n{TIMESTAMP}
```

- Next 서버와 백엔드는 동일한 `CLIENT_IP_SIGNING_SECRET`을 사용합니다.
- 백엔드는 기본값 기준 현재 시각과 60초를 초과하여 차이 나는 요청을 거절합니다.
- 헤더 누락, 잘못된 서명, 만료 시각, 유효하지 않은 IP는 `403 Forbidden`으로 응답합니다.

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_1",
  "message": "요청에 성공했습니다.",
  "data": {}
}
```

### 공통 실패 코드

| HTTP 상태 | code | message | 발생 조건 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | Bean Validation 또는 요청 형식 검증 실패 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않음 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | 필요한 권한이 없거나 Next IP 전달 서명 검증 실패 |
| `404 Not Found` | `COMMON_404_1` | 요청한 리소스를 찾을 수 없습니다. | 매핑되지 않은 리소스 요청 |
| `500 Internal Server Error` | `COMMON_500_1` | 서버 내부 오류가 발생했습니다. | 처리되지 않은 서버 오류 |

### 날짜·시간과 Enum

- 날짜는 `yyyy-MM-dd` 형식입니다.
- `LocalTime`은 ISO-8601 시간 문자열, `LocalDateTime`은 오프셋 없는 ISO-8601 날짜·시간 문자열입니다.
- `OffsetDateTime`은 `Asia/Seoul` 오프셋을 포함한 ISO-8601 날짜·시간 문자열입니다.
- 수정 요청 유형: `CLOCK_IN_TIME`, `CLOCK_OUT_TIME`, `MISSING_RECORD`, `CLOCK_IN_NOTE`, `CLOCK_OUT_NOTE`
- 수정 요청 상태: `PENDING`, `APPROVED`, `REJECTED`
- 퇴근 유형: `NORMAL`, `OVERTIME`

## API 목록

| 구분 | Method | Endpoint | 인증·권한 |
| --- | --- | --- | --- |
| 출퇴근 | `POST` | `/api/attendance/check-ins` | 인증 사용자 |
| 출퇴근 | `POST` | `/api/attendance/check-outs` | 인증 사용자 |
| 근무 정책 | `PUT` | `/api/attendance/policies` | `ATTENDANCE:POLICY_MANAGE` |
| 팀 조회 | `GET` | `/api/attendance/team/today` | `ATTENDANCE:READ` |
| 주간 조회 | `GET` | `/api/attendance/employees/weekly` | `ATTENDANCE:READ` |
| 주간 조회 | `GET` | `/api/attendance/employees/{userId}/weekly` | `ATTENDANCE:READ` |
| 개인 조회 | `GET` | `/api/attendance/me/monthly` | 인증 사용자 |
| 개인 조회 | `GET` | `/api/attendance/me/today` | 인증 사용자 |
| 개인 조회 | `GET` | `/api/attendance/me/dashboard` | 인증 사용자 |
| 개인 조회 | `GET` | `/api/attendance/me/days/{date}` | 인증 사용자 |
| 개인 수정 요청 | `POST` | `/api/attendance/me/correction-requests` | 인증 사용자 |
| 개인 수정 요청 | `GET` | `/api/attendance/me/correction-requests` | 인증 사용자 |
| 개인 수정 요청 | `GET` | `/api/attendance/me/correction-requests/{requestId}` | 인증 사용자 |
| 관리자 수정 요청 | `GET` | `/api/attendance/correction-requests` | `ATTENDANCE:CORRECTION_READ` |
| 관리자 수정 요청 | `GET` | `/api/attendance/correction-requests/{requestId}` | `ATTENDANCE:CORRECTION_READ` |
| 관리자 수정 요청 | `POST` | `/api/attendance/correction-requests/{requestId}/approve` | `ATTENDANCE:CORRECTION_PROCESS` |
| 관리자 수정 요청 | `POST` | `/api/attendance/correction-requests/{requestId}/reject` | `ATTENDANCE:CORRECTION_PROCESS` |
| Wi-Fi IP | `GET` | `/api/attendance/wifi-ips/current` | 인증 사용자 |
| Wi-Fi IP | `GET` | `/api/attendance/wifi-ips` | `ATTENDANCE:WIFI_IP_MANAGE` |
| Wi-Fi IP | `POST` | `/api/attendance/wifi-ips` | `ATTENDANCE:WIFI_IP_MANAGE` |
| Wi-Fi IP | `DELETE` | `/api/attendance/wifi-ips/{wifiIpId}` | `ATTENDANCE:WIFI_IP_MANAGE` |
| 연가·재직 | `GET` | `/api/leaves/me/summary` | 인증 사용자 |
| 연가·재직 | `GET` | `/api/users/me/employment-summary` | 인증 사용자 |

## 출퇴근

### 출근 체크인

`POST /api/attendance/check-ins`
인증·권한: 인증 사용자
Notion 원문: [출근 체크인](https://app.notion.com/p/3b313f22e20281749caffc400a6b6136)

Request Body

```json
{
  "clockInNote": "교통 지연으로 늦게 출근했습니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `clockInNote` | `String` | 아니요 | 출근 메모. 최대 255자이며, 지각으로 판정되면 비어 있을 수 없음 |

성공: `201 Created`, `ATTENDANCE_201_1`, `출근이 등록되었습니다.`

```json
{
  "status": 201,
  "code": "ATTENDANCE_201_1",
  "message": "출근이 등록되었습니다.",
  "data": {
    "attendanceId": 1,
    "workDate": "2026-08-13",
    "clockInAt": "2026-08-13T09:05:00",
    "clockInNote": "교통 지연으로 늦게 출근했습니다.",
    "status": "LATE"
  }
}
```

비즈니스 규칙

- 서버가 감지한 IP가 등록된 허용 IP인지 확인합니다.
- 현재 정책의 출근 시각과 지각 유예 시간을 기준으로 `NORMAL` 또는 `LATE`를 판정합니다.
- 같은 사용자·근무일의 출근 기록은 한 번만 생성됩니다.

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400` | `ATTENDANCE_400_4` | 지각인데 출근 사유가 없음 |
| `400` | `ATTENDANCE_400_5` | 출근 메모가 255자를 초과함 |
| `400` | `ACADEMY_400_1` | 감지된 IP 형식이 유효하지 않음 |
| `403` | `ATTENDANCE_403_2` | 인증 사용자 식별자를 확인할 수 없음 |
| `403` | `ATTENDANCE_403_3` | 등록되지 않은 IP에서 요청함 |
| `404` | `ATTENDANCE_404_1` | 근무시간 정책이 없음 |
| `409` | `ATTENDANCE_409_1` | 해당 근무일 출근이 이미 등록됨 |

### 퇴근 체크아웃

`POST /api/attendance/check-outs`
인증·권한: 인증 사용자
Notion 원문: [퇴근 체크아웃](https://app.notion.com/p/3b313f22e202817da279c3b4e3f1f3ab)

Request Body

```json
{
  "clockOutType": "NORMAL",
  "clockOutNote": null
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `clockOutType` | `ClockOutType` | 예 | `NORMAL` 또는 `OVERTIME` |
| `clockOutNote` | `String` | 아니요 | 퇴근 메모. 최대 255자이며, `OVERTIME`이면 비어 있을 수 없음 |

성공: `200 OK`, `ATTENDANCE_200_2`, `퇴근이 등록되었습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_2",
  "message": "퇴근이 등록되었습니다.",
  "data": {
    "attendanceId": 1,
    "workDate": "2026-08-13",
    "clockInAt": "2026-08-13T09:05:00",
    "clockOutAt": "2026-08-13T18:00:00",
    "clockOutType": "NORMAL",
    "clockOutNote": null,
    "status": "LATE"
  }
}
```

비즈니스 규칙

- 서버가 감지한 IP가 등록된 허용 IP인지 확인합니다.
- 당일 또는 전일 이후의 가장 최근 미퇴근 기록을 처리합니다.

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400` | `ATTENDANCE_400_7` | 퇴근 메모가 255자를 초과함 |
| `400` | `ATTENDANCE_400_8` | 퇴근 시각이 출근 시각보다 빠름 |
| `400` | `ATTENDANCE_400_9` | 초과근무인데 퇴근 사유가 없음 |
| `400` | `ACADEMY_400_1` | 감지된 IP 형식이 유효하지 않음 |
| `403` | `ATTENDANCE_403_4` | 인증 사용자 식별자를 확인할 수 없음 |
| `403` | `ATTENDANCE_403_5` | 등록되지 않은 IP에서 요청함 |
| `404` | `ATTENDANCE_404_2` | 처리할 미퇴근 출근 기록이 없음 |
| `409` | `ATTENDANCE_409_3` | 이미 퇴근 처리됨 |

## 근무 정책과 조직 조회

### 근무시간 정책 저장

`PUT /api/attendance/policies`
필요 권한: `ATTENDANCE:POLICY_MANAGE`
Notion 원문: [근무시간 정책 저장](https://app.notion.com/p/3b213f22e20281198dddd55a54e2d268)

Request Body

```json
{
  "defaultStartTime": "09:00:00",
  "defaultEndTime": "18:00:00",
  "lateGraceMinutes": 10,
  "weekdayExceptionEnabled": true,
  "weekdays": [
    {
      "dayOfWeek": 1,
      "isWorkday": true,
      "startTime": null,
      "endTime": null
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `defaultStartTime` | `LocalTime` | 예 | 기본 출근 시간 |
| `defaultEndTime` | `LocalTime` | 예 | 기본 퇴근 시간 |
| `lateGraceMinutes` | `int` | 예 | 지각 유예 시간, 0~180분 |
| `weekdayExceptionEnabled` | `Boolean` | 예 | 요일별 근무 설정 사용 여부 |
| `weekdays` | `Array` | 아니요 | 요일별 설정 목록 |
| `weekdays[].dayOfWeek` | `Integer` | 예 | 1(월요일)~7(일요일) |
| `weekdays[].isWorkday` | `Boolean` | 예 | 근무일 여부 |
| `weekdays[].startTime` | `LocalTime` | 아니요 | 미입력 시 기본 출근 시간 사용 |
| `weekdays[].endTime` | `LocalTime` | 아니요 | 미입력 시 기본 퇴근 시간 사용 |

성공: `200 OK`, `ATTENDANCE_200_1`, `근무시간 정책이 저장되었습니다.`
응답 `data`는 요청 필드와 함께 `policyId`를 반환합니다.

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400` | `ATTENDANCE_400_1` | 정책의 시간 범위 또는 조합이 유효하지 않음 |
| `400` | `ATTENDANCE_400_2` | 요일별 설정이 유효하지 않음 |
| `400` | `ATTENDANCE_400_3` | 같은 요일을 중복 설정함 |
| `403` | `COMMON_403_1` | 정책 관리 권한이 없음 |

### 오늘 팀 근태 현황 조회

`GET /api/attendance/team/today`
필요 권한: `ATTENDANCE:READ`
Notion 원문: [오늘 팀 근태 현황 조회](https://app.notion.com/p/3b313f22e20281c08bb5dda69f93042e)

요청 파라미터와 Body는 없습니다.

성공: `200 OK`, `ATTENDANCE_200_3`, `오늘 팀 근태 현황을 조회했습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_3",
  "message": "오늘 팀 근태 현황을 조회했습니다.",
  "data": {
    "date": "2026-08-13",
    "dayOfWeek": "목",
    "regularWorkStartTime": "09:00",
    "regularWorkEndTime": "18:00",
    "summary": {
      "presentCount": 6,
      "absentCount": 1,
      "offCount": 0,
      "leaveCount": 0
    },
    "employees": [
      {
        "userId": 2,
        "name": "김지수",
        "status": "PRESENT",
        "checkInTime": "08:52"
      }
    ]
  }
}
```

- 승인된 휴가 기간은 `LEAVE`로 표시하고 `leaveCount`에 포함합니다.
- 직원 상태는 `PRESENT`, `ABSENT`, `OFF`, `LEAVE` 중 하나입니다.
- 슈퍼 어드민(`accountType=ADMIN`, `adminScope=PLATFORM`)은 조회 대상에서 제외합니다.
- 근무시간 정책이 없으면 `404 ATTENDANCE_404_1`입니다.

### 전 직원 주간 출결 현황 조회

`GET /api/attendance/employees/weekly`
필요 권한: `ATTENDANCE:READ`
Notion 원문: [전 직원 주간 출결 현황 조회](https://app.notion.com/p/3b513f22e20281e58e12fafc687b8ad9)

| Query | 타입 | 필수 | 기본값·제약 | 설명 |
| --- | --- | --- | --- | --- |
| `date` | `LocalDate` | 예 | `yyyy-MM-dd` | 조회할 주에 포함된 날짜 |
| `keyword` | `String` | 아니요 | 없음 | 직원 이름 검색어 |
| `status` | `MyAttendanceDayStatus` | 아니요 | 없음 | 주중 하루라도 해당 상태인 직원 필터 |
| `page` | `int` | 아니요 | `0`, 최소 0 | 페이지 번호 |
| `size` | `int` | 아니요 | `20`, 1~100 | 페이지 크기 |

성공: `200 OK`, `ATTENDANCE_200_16`, `주간 전 직원 출결 현황을 조회했습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_16",
  "message": "주간 전 직원 출결 현황을 조회했습니다.",
  "data": {
    "week": { "startDate": "2026-08-10", "endDate": "2026-08-16" },
    "scheduledWorkDays": 5,
    "employees": {
      "content": [
        {
          "userId": 27,
          "name": "윤예진",
          "roleName": "강사",
          "attendedDays": 1,
          "scheduledWorkDays": 5,
          "days": [
            {
              "date": "2026-08-13",
              "status": "NORMAL",
              "clockInAt": "2026-08-13T09:00:00",
              "clockOutAt": "2026-08-13T18:00:00"
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

- 조회 주는 월요일부터 일요일까지입니다.
- `attendedDays`에는 `NORMAL`, `LATE`만 포함합니다.
- `roleName`은 사용자의 `role.name`이며 역할이 없으면 `null`입니다.
- 슈퍼 어드민(`accountType=ADMIN`, `adminScope=PLATFORM`)은 조회 대상에서 제외합니다.
- 유효하지 않은 조회 기간은 `400 ATTENDANCE_400_12`, 정책이 없으면 `404 ATTENDANCE_404_1`입니다.

### 특정 직원 주간 출결 상세 조회

`GET /api/attendance/employees/{userId}/weekly`
필요 권한: `ATTENDANCE:READ`
Notion 원문: [특정 직원 주간 출결 상세 조회](https://app.notion.com/p/3b513f22e20281a38f37e9b0d52a7849)

| 구분 | 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- | --- |
| Path | `userId` | `Long` | 예 | 조회할 직원 ID |
| Query | `date` | `LocalDate` | 예 | 조회할 주에 포함된 날짜 |

성공: `200 OK`, `ATTENDANCE_200_17`, `직원의 주간 출결 상세를 조회했습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_17",
  "message": "직원의 주간 출결 상세를 조회했습니다.",
  "data": {
    "employee": { "userId": 27, "name": "윤예진", "roleName": "강사" },
    "week": { "startDate": "2026-08-10", "endDate": "2026-08-16" },
    "days": [
      {
        "date": "2026-08-13",
        "status": "NORMAL",
        "clockInAt": "2026-08-13T09:00:00",
        "clockOutAt": "2026-08-13T18:00:00"
      }
    ],
    "weeklySummary": { "scheduledWorkDays": 5, "attendedDays": 1 }
  }
}
```

- 유효하지 않은 조회 기간은 `400 ATTENDANCE_400_12`입니다.
- 정책이 없으면 `404 ATTENDANCE_404_1`, 대상 직원을 찾을 수 없으면 `404 ATTENDANCE_404_5`입니다.

## 내 근태 조회

### 내 월별 근태 조회

`GET /api/attendance/me/monthly`
인증·권한: 인증 사용자
Notion 원문: [내 월별 근태 조회](https://app.notion.com/p/3b513f22e202810b8c9dcbcbadf48fc4)

| Query | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| `year` | `int` | 예 | 1~9999 |
| `month` | `int` | 예 | 1~12 |

성공: `200 OK`, `ATTENDANCE_200_4`, `월별 근태를 조회했습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_4",
  "message": "월별 근태를 조회했습니다.",
  "data": {
    "year": 2026,
    "month": 8,
    "days": [
      {
        "date": "2026-08-13",
        "status": "LATE",
        "clockInAt": "09:05:12",
        "clockOutAt": "18:02:31"
      }
    ]
  }
}
```

- 입사일부터 오늘까지의 범위만 계산합니다.
- 재직 정보가 없으면 `404 ATTENDANCE_404_3`, 정책이 없으면 `404 ATTENDANCE_404_1`입니다.
- 유효하지 않은 연월은 `400 ATTENDANCE_400_12`입니다.

### 내 오늘 근태 조회

`GET /api/attendance/me/today`
인증·권한: 인증 사용자
Notion 원문: [내 오늘 근태 조회](https://app.notion.com/p/3b513f22e202812e9fdbe245869ed773)

요청 파라미터와 Body는 없습니다.

성공: `200 OK`, `ATTENDANCE_200_5`, `오늘 근태를 조회했습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_5",
  "message": "오늘 근태를 조회했습니다.",
  "data": {
    "date": "2026-08-13",
    "workStartTime": "09:00:00",
    "workEndTime": "18:00:00",
    "clockInAt": "2026-08-13T09:05:12+09:00",
    "clockOutAt": null,
    "status": "LATE",
    "serverTime": "2026-08-13T14:57:21+09:00"
  }
}
```

- `serverTime`을 기준으로 프론트엔드가 경과 시간 타이머를 계산합니다.
- 정책이 없으면 `404 ATTENDANCE_404_1`입니다.

### 내 근태 대시보드 조회

`GET /api/attendance/me/dashboard`
인증·권한: 인증 사용자
Notion 원문: [내 근태 대시보드 조회](https://app.notion.com/p/3b513f22e20281d4948ed15d811efd6a)

| Query | 타입 | 필수 | 제약 |
| --- | --- | --- | --- |
| `year` | `int` | 예 | 1~9999 |
| `month` | `int` | 예 | 1~12 |

성공: `200 OK`, `ATTENDANCE_200_8`, `내 근태 대시보드를 조회했습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_8",
  "message": "내 근태 대시보드를 조회했습니다.",
  "data": {
    "calendar": { "year": 2026, "month": 8, "days": [] },
    "today": {
      "date": "2026-08-13",
      "workStartTime": "09:00:00",
      "workEndTime": "18:00:00",
      "clockInAt": null,
      "clockOutAt": null,
      "status": "UNRECORDED",
      "serverTime": "2026-08-13T09:00:00+09:00"
    },
    "leave": {
      "totalDays": 15,
      "usedDays": 5,
      "pendingDays": 1,
      "remainingDays": 10,
      "nextGrantDate": "2027-03-01"
    },
    "employment": { "hireDate": "2025-06-17", "tenureDays": 423 }
  }
}
```

- 월별 근태, 오늘 근태, 연가, 재직 조회 결과를 조합합니다.
- 각 하위 조회에서 발생하는 `ATTENDANCE_400_12`, `ATTENDANCE_404_1`, `ATTENDANCE_404_3`이 그대로 전파될 수 있습니다.

### 특정 날짜 내 근태 상세 조회

`GET /api/attendance/me/days/{date}`
인증·권한: 인증 사용자
Notion 원문: [특정 날짜 내 근태 상세 조회](https://app.notion.com/p/3b513f22e202813795d7c103066578d5)

| Path | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `date` | `LocalDate` | 예 | 조회할 날짜 |

성공: `200 OK`, `ATTENDANCE_200_9`, `선택한 날짜의 근태가 조회되었습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_9",
  "message": "선택한 날짜의 근태가 조회되었습니다.",
  "data": {
    "date": "2026-08-13",
    "clockInAt": "2026-08-13T09:05:00",
    "clockOutAt": "2026-08-13T18:00:00",
    "clockInNote": null,
    "clockOutNote": null,
    "correctionRequestPending": false
  }
}
```

- 기록이 없으면 출퇴근 시각과 메모는 `null`입니다.
- 미래 날짜는 `400 ATTENDANCE_400_13`입니다.

## 내 근태 수정 요청

### 근태 수정 요청 등록

`POST /api/attendance/me/correction-requests`
인증·권한: 인증 사용자
Notion 원문: [근태 수정 요청 등록](https://app.notion.com/p/3b513f22e202816a8a55e9c7ea8ece34)

Request Body

```json
{
  "date": "2026-08-13",
  "type": "CLOCK_IN_TIME",
  "requestedClockInTime": "09:00:00",
  "requestedClockOutTime": null,
  "requestedClockInNote": null,
  "requestedClockOutNote": null,
  "reason": "출근 버튼을 늦게 눌렀습니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `date` | `LocalDate` | 예 | 수정 대상 날짜 |
| `type` | `AttendanceCorrectionType` | 예 | 수정 요청 유형 |
| `requestedClockInTime` | `LocalTime` | 조건부 | 출근 시각 수정 또는 누락 기록 생성 시 사용 |
| `requestedClockOutTime` | `LocalTime` | 조건부 | 퇴근 시각 수정 또는 누락 기록 생성 시 사용 |
| `requestedClockInNote` | `String` | 조건부 | 출근 메모 수정값, 최대 255자 |
| `requestedClockOutNote` | `String` | 조건부 | 퇴근 메모 수정값, 최대 255자 |
| `reason` | `String` | 예 | 요청 사유, 공백 불가, 최대 500자 |

성공: `201 Created`, `ATTENDANCE_201_2`, `근태 수정 요청이 등록되었습니다.`
응답에는 `requestId`, 원본 값, 요청 값, 사유, 요청·처리 시각과 상태를 반환합니다.

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400` | `ATTENDANCE_400_12` | 요청 유형과 입력 필드 조합이 유효하지 않음 |
| `400` | `ATTENDANCE_400_13` | 미래 날짜를 요청함 |
| `404` | `ATTENDANCE_404_3` | 기존 기록이 필요한 유형인데 근태 기록이 없음 |
| `409` | `ATTENDANCE_409_7` | 같은 날짜에 처리 중인 요청이 있음 |

### 내 근태 수정 요청 목록 조회

`GET /api/attendance/me/correction-requests`
인증·권한: 인증 사용자
Notion 원문: [내 근태 수정 요청 목록 조회](https://app.notion.com/p/3b513f22e20281f78c0ac49e70d0bdde)

요청 파라미터와 Body는 없습니다.
성공: `200 OK`, `ATTENDANCE_200_10`, `내 근태 수정 요청 목록이 조회되었습니다.`

- 응답 `data`는 `AttendanceCorrectionResponse[]`입니다.
- 각 항목은 `requestId`, `date`, `type`, `status`, 원본·요청 출퇴근 시각과 메모, `reason`, `requestedAt`, `processedAt`, `rejectionReason`을 포함합니다.
- 본인 요청만 최신 요청 시각순으로 반환합니다.

### 내 근태 수정 요청 상세 조회

`GET /api/attendance/me/correction-requests/{requestId}`
인증·권한: 인증 사용자
Notion 원문: [내 근태 수정 요청 상세 조회](https://app.notion.com/p/3b513f22e20281bfb388e732cba8c195)

| Path | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `requestId` | `Long` | 예 | 근태 수정 요청 ID |

성공: `200 OK`, `ATTENDANCE_200_11`, `내 근태 수정 요청이 조회되었습니다.`
응답 필드는 목록 항목과 같습니다.

- 존재하지 않거나 본인 소유가 아닌 요청은 모두 `404 ATTENDANCE_404_4`로 처리합니다.

## 관리자 근태 수정 요청

관리자 응답의 `requester`는 `userId`, `name`, `roleName`을 포함합니다. `roleName`은 `role.name`이며 역할이 없거나 요청자 조회 결과가 없으면 `null`일 수 있습니다.

### 관리자 근태 수정 요청 목록 조회

`GET /api/attendance/correction-requests`
필요 권한: `ATTENDANCE:CORRECTION_READ`
Notion 원문: [관리자 근태 수정 요청 목록 조회](https://app.notion.com/p/3b513f22e2028185a3bdc4247e545499)

| Query | 타입 | 필수 | 기본값·제약 | 설명 |
| --- | --- | --- | --- | --- |
| `status` | `AttendanceCorrectionStatus` | 아니요 | 없음 | 상태 필터 |
| `page` | `int` | 아니요 | `0`, 최소 0 | 페이지 번호 |
| `size` | `int` | 아니요 | `30`, 1~100 | 페이지 크기 |

성공: `200 OK`, `ATTENDANCE_200_12`, `근태 수정 요청 목록이 조회되었습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_12",
  "message": "근태 수정 요청 목록이 조회되었습니다.",
  "data": {
    "content": [
      {
        "requestId": 101,
        "requester": { "userId": 10, "name": "이민준", "roleName": "강사" },
        "workDate": "2026-08-13",
        "type": "CLOCK_IN_TIME",
        "status": "PENDING",
        "originalClockInAt": "2026-08-13T09:35:00",
        "originalClockOutAt": null,
        "originalClockInNote": null,
        "originalClockOutNote": null,
        "requestedClockInAt": "2026-08-13T09:05:00",
        "requestedClockOutAt": null,
        "requestedClockInNote": null,
        "requestedClockOutNote": null,
        "reason": "실제 출근 시각 정정",
        "requestedAt": "2026-08-13T20:15:00",
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

### 관리자 근태 수정 요청 상세 조회

`GET /api/attendance/correction-requests/{requestId}`
필요 권한: `ATTENDANCE:CORRECTION_READ`
Notion 원문: [관리자 근태 수정 요청 상세 조회](https://app.notion.com/p/3b513f22e202811f906bc019dea3f1cc)

| Path | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `requestId` | `Long` | 예 | 근태 수정 요청 ID |

성공: `200 OK`, `ATTENDANCE_200_13`, `근태 수정 요청이 조회되었습니다.`
응답 `data`는 목록의 `content[]` 항목과 같습니다. 요청이 없으면 `404 ATTENDANCE_404_4`입니다.

### 관리자 근태 수정 요청 승인

`POST /api/attendance/correction-requests/{requestId}/approve`
필요 권한: `ATTENDANCE:CORRECTION_PROCESS`
Notion 원문: [관리자 근태 수정 요청 승인](https://app.notion.com/p/3b513f22e2028175a51be1c76829e5f3)

| Path | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `requestId` | `Long` | 예 | 승인할 요청 ID |

Request Body는 없습니다.
성공: `200 OK`, `ATTENDANCE_200_14`, `근태 수정 요청이 승인되었습니다.`, `data: null`

- 요청을 비관적 쓰기 락으로 조회하고, 근태 기록 반영과 승인 처리를 한 트랜잭션에서 수행합니다.

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400` | `ATTENDANCE_400_12` | 요청 유형과 현재 기록의 조합이 유효하지 않음 |
| `404` | `ATTENDANCE_404_1` | 출근 시각 판정에 필요한 정책이 없음 |
| `404` | `ATTENDANCE_404_3` | 기존 기록이 필요한 유형인데 기록이 없음 |
| `404` | `ATTENDANCE_404_4` | 요청을 찾을 수 없음 |
| `409` | `ATTENDANCE_409_2` | 출근 시각을 반영할 날짜가 근무일이 아님 |
| `409` | `ATTENDANCE_409_8` | 이미 처리된 요청임 |

### 관리자 근태 수정 요청 반려

`POST /api/attendance/correction-requests/{requestId}/reject`
필요 권한: `ATTENDANCE:CORRECTION_PROCESS`
Notion 원문: [관리자 근태 수정 요청 반려](https://app.notion.com/p/3b513f22e20281269627c1741873fca4)

Request Body

```json
{
  "reason": "제출 내용으로 실제 시간을 확인하기 어렵습니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `String` | 예 | 공백 불가, 최대 500자 |

성공: `200 OK`, `ATTENDANCE_200_15`, `근태 수정 요청이 반려되었습니다.`, `data: null`

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400` | `COMMON_400_1` | Bean Validation 실패 |
| `400` | `ATTENDANCE_400_14` | 도메인 검증에서 반려 사유가 유효하지 않음 |
| `404` | `ATTENDANCE_404_4` | 요청을 찾을 수 없음 |
| `409` | `ATTENDANCE_409_8` | 이미 처리된 요청임 |

## Wi-Fi IP

### 현재 접속 IP 조회

`GET /api/attendance/wifi-ips/current`
인증·권한: 인증 사용자
Notion 원문: [현재 접속 IP 조회](https://app.notion.com/p/3b213f22e202813d9b60f8766bf16ff5)

요청 파라미터와 Body는 없습니다.
성공: `200 OK`, `ACADEMY_200_1`, `현재 접속 IP가 조회되었습니다.`

```json
{
  "status": 200,
  "code": "ACADEMY_200_1",
  "message": "현재 접속 IP가 조회되었습니다.",
  "data": { "ipAddress": "203.0.113.10" }
}
```

### 등록된 와이파이 IP 목록 조회

`GET /api/attendance/wifi-ips`
필요 권한: `ATTENDANCE:WIFI_IP_MANAGE`
Notion 원문: [등록된 와이파이 IP 목록 조회](https://app.notion.com/p/3b313f22e2028131bd0dfcbd390d42f4)

요청 파라미터와 Body는 없습니다.
성공: `200 OK`, `ACADEMY_200_3`, `등록된 와이파이 IP 목록을 조회했습니다.`

```json
{
  "status": 200,
  "code": "ACADEMY_200_3",
  "message": "등록된 와이파이 IP 목록을 조회했습니다.",
  "data": [
    {
      "wifiIpId": 1,
      "ipAddress": "203.0.113.10",
      "note": "학원 공유기",
      "createdAt": "2026-08-13T10:30:00"
    }
  ]
}
```

- 등록 결과가 없으면 `200 OK`와 빈 배열을 반환합니다.

### 학원 와이파이 IP 등록

`POST /api/attendance/wifi-ips`
필요 권한: `ATTENDANCE:WIFI_IP_MANAGE`
Notion 원문: [학원 와이파이 IP 등록](https://app.notion.com/p/3b213f22e202817c82c2c672857c2cb4)

Request Body

```json
{
  "confirmedIpAddress": "203.0.113.10",
  "note": "학원 공유기"
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `confirmedIpAddress` | `String` | 예 | 현재 접속 IP 조회 API에서 확인한 IP, 최대 45자 |
| `note` | `String` | 아니요 | IP 구분 메모, 최대 100자 |

성공: `201 Created`, `ACADEMY_201_1`, `와이파이 IP가 등록되었습니다.`
응답 `data`는 `wifiIpId`, `ipAddress`, `note`, `createdAt`을 포함합니다.

| HTTP 상태 | code | 발생 조건 |
| --- | --- | --- |
| `400` | `ACADEMY_400_1` | IP 주소 형식이 유효하지 않음 |
| `400` | `ACADEMY_400_2` | 메모가 100자를 초과함 |
| `409` | `ACADEMY_409_1` | 이미 등록된 IP임 |
| `409` | `ACADEMY_409_2` | 확인한 IP와 서버가 감지한 IP가 다름 |

### 학원 와이파이 IP 삭제

`DELETE /api/attendance/wifi-ips/{wifiIpId}`
필요 권한: `ATTENDANCE:WIFI_IP_MANAGE`
Notion 원문: [학원 와이파이 IP 삭제](https://app.notion.com/p/3b313f22e2028109af71f72fde8fa54b)

| Path | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `wifiIpId` | `Long` | 예 | 삭제할 Wi-Fi IP ID |

성공: `200 OK`, `ACADEMY_200_2`, `와이파이 IP가 삭제되었습니다.`, `data: null`
대상이 없으면 `404 ACADEMY_404_1`입니다.

## 연가·재직

### 내 연가 현황 조회

`GET /api/leaves/me/summary`
인증·권한: 인증 사용자
Notion 원문: [내 연가 현황 조회](https://app.notion.com/p/3b513f22e2028160932de61d246054e4)

요청 파라미터와 Body는 없습니다.
성공: `200 OK`, `ATTENDANCE_200_6`, `연가 현황을 조회했습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_6",
  "message": "연가 현황을 조회했습니다.",
  "data": {
    "totalDays": 15,
    "usedDays": 3,
    "pendingDays": 2,
    "remainingDays": 12,
    "nextGrantDate": "2027-03-01"
  }
}
```

- `pendingDays`는 결재 대기 중인 연가 일수입니다.
- `nextGrantDate`는 다음 지급일이 없으면 `null`일 수 있습니다.

### 내 재직 정보 조회

`GET /api/users/me/employment-summary`
인증·권한: 인증 사용자
Notion 원문: [내 재직 정보 조회](https://app.notion.com/p/3b513f22e20281768e77cc9ae9e30510)

요청 파라미터와 Body는 없습니다.
성공: `200 OK`, `ATTENDANCE_200_7`, `재직 정보를 조회했습니다.`

```json
{
  "status": 200,
  "code": "ATTENDANCE_200_7",
  "message": "재직 정보를 조회했습니다.",
  "data": {
    "hireDate": "2025-06-17",
    "tenureDays": 423
  }
}
```

- `tenureDays`는 출근일수가 아니라 입사일부터 오늘까지의 달력상 경과 일수입니다.
- 재직 정보가 없으면 `404 ATTENDANCE_404_3`입니다.

## Notion 명세 동기화 이력

2026-08-13에 아래 차이를 현재 코드 기준으로 Notion 원문에 반영했습니다.

| 항목 | 기존 Notion 명세 | 동기화한 코드 기준 |
| --- | --- | --- |
| 출근·퇴근 권한 | `ATTENDANCE:CHECK_IN`, `ATTENDANCE:CHECK_OUT` 권한 적용으로 기록됨 | 두 Controller 모두 `isAuthenticated()` 적용 |
| 현재 접속 IP 인증 | 원문 본문 일부에 인증 불필요로 기록됨 | 메서드 권한 어노테이션은 없지만 전역 Security 설정으로 인증 필요 |
| 관리자 수정 요청 학원 범위 | 소속 학원 범위 조회로 설명됨 | 현재 Service/Repository 호출은 요청자 ID나 학원 ID를 전달하지 않음 |
| Wi-Fi IP 학원 범위 | 원장 소유 학원 범위로 설명됨 | 현재 Service/Repository는 전역 IP 목록·ID 기준으로 처리하며 요청자 ID를 조회 조건으로 사용하지 않음 |
| Wi-Fi 등록 `note` | 원문 필드 표에 필수로 표시됨 | Request DTO에서는 선택값이며 최대 100자 |
| 선택 날짜 시각 예시 | 원문에 `HH:mm`으로 표시됨 | Response DTO 타입은 `LocalDateTime`이므로 날짜를 포함한 ISO-8601 문자열 |
| 근무시간 정책 범위 | 학원별 정책으로 설명됨 | 전역 현재 정책 하나를 생성하거나 갱신 |
| 팀·주간 조회 범위 | 소속 학원 직원 조회로 설명됨 | 현재 조회 SQL은 학원 조건 없이 활성 사용자를 조회 |
| 수정 요청 등록 필드 | 선택 필드가 누락되고 `requestedClockInTime`이 항상 필수로 표시됨 | 전체 요청 필드와 유형별 조건부 입력을 반영 |
| Wi-Fi 등록 응답 | `createdAt`이 누락됨 | `AcademyWifiIpResponse`의 `createdAt`을 반영 |

## Sources

- [Notion API 명세서 데이터베이스](https://app.notion.com/p/3b213f22e202808a8a1bee21d6a5a76d)
- 각 API의 직접 원문 링크는 해당 API 제목 아래에 표시했습니다.
