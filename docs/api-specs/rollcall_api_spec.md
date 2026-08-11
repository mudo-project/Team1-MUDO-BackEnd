# 출결(Rollcall) API 명세서 — 문자 발송

> 이 파일은 출결 도메인 전체가 아니라 **문자 발송 관련 2개 엔드포인트만** 다룬다(2026-08-10, Solapi 실제 연동 검증 완료 시점 기준 작성). 로스터 조회/저장/엑셀 다운로드 등 기존 엔드포인트는 아직 이 형식의 명세서가 없다.
> 공통 성공 응답 포맷: `{ "status", "code", "message", "data" }` (`GlobalApiResponse`).
> 공통 실패 응답 포맷: `{ "timestamp", "status", "code", "message", "traceId", "details" }`
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더와 `ROLLCALL:MANAGE` 권한이 필요합니다 (미인증 시 `401 COMMON_401_1`, 권한 없으면 `403 COMMON_403_1`).
> `{lectureId}`가 요청자 학원 소속 강의가 아니거나 존재하지 않으면 `404 ROLLCALL_404_2`.

---

## 1. 문자 발송 대상 조회

`GET /api/rollcall/lectures/{lectureId}/attendance/message-candidates`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `lectureId` | 조회할 강의 ID입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `date` | `LocalDate` | `true` | 조회할 출결 날짜입니다(`yyyy-MM-dd`). |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 발송 대상 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "ROLLCALL_200_3",
  "message": "발송 대상 조회에 성공했습니다.",
  "data": [
    {
      "studentId": 101,
      "studentName": "김민수",
      "status": "ABSENT",
      "parentPhone": "010-3333-4444",
      "matchedTemplateId": 3,
      "matchedTemplateName": "결석 안내",
      "eligible": true
    }
  ]
}
```

### Response Field

| name | 설명 |
| --- | --- |
| `data[].studentId` | 학생 ID입니다. |
| `data[].studentName` | 학생 이름입니다. |
| `data[].status` | 해당 날짜의 출결 상태입니다(`PRESENT`/`ABSENT`/`LATE`/`ONLINE`/`ETC`). 출결이 저장되지 않은 학생은 후보 목록 자체에 포함되지 않습니다. |
| `data[].parentPhone` | 학부모 연락처입니다. |
| `data[].matchedTemplateId` | 해당 출결 상태에 매칭되는 문자 템플릿 ID입니다. 매칭되는 템플릿이 없으면 `null`입니다. |
| `data[].matchedTemplateName` | 매칭되는 문자 템플릿 이름입니다. 매칭되는 템플릿이 없으면 `null`입니다. |
| `data[].eligible` | 실제 발송 가능 여부입니다(매칭 템플릿이 있으면 `true`). `false`인 항목을 발송 요청에 포함해도 발송 없이 실패로 처리됩니다. |

> 참고: 이 API는 조회만 하고 실제 SMS를 발송하지 않습니다(2번 API 참고).

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `date` 파라미터 누락/형식 오류 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | `ROLLCALL:MANAGE` 권한 없음 |
| `404 Not Found` | `ROLLCALL_404_2` | 강의를 찾을 수 없습니다. | `lectureId`가 요청자 학원 소속 강의가 아니거나 존재하지 않음 |

---

## 2. 출결 안내 문자 발송

`POST /api/rollcall/lectures/{lectureId}/attendance/message-candidates/send`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `lectureId` | 발송 대상 강의 ID입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `date` | `LocalDate` | `true` | 발송 기준 출결 날짜입니다(`yyyy-MM-dd`). |

Request Body
```json
{
  "studentIds": [101, 102]
}
```

| name | type | required | 설명 |
| --- | --- | --- | --- |
| `studentIds` | `List<Long>` | `true` | 문자를 발송할 학생 ID 목록입니다. 최소 1개 이상이어야 합니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 발송 요청 처리 성공(학생별 성공/실패는 `data`로 구분) |

Response Body
```json
{
  "status": 200,
  "code": "ROLLCALL_200_5",
  "message": "문자 발송을 완료했습니다.",
  "data": [
    {
      "studentId": 101,
      "studentName": "김민수",
      "sent": true,
      "failureReason": null
    },
    {
      "studentId": 102,
      "studentName": "이서연",
      "sent": false,
      "failureReason": "발송 대상이 아닙니다(출결 미입력 또는 매칭되는 템플릿 없음)."
    }
  ]
}
```

### Response Field

| name | 설명 |
| --- | --- |
| `data[].studentId` | 학생 ID입니다. |
| `data[].studentName` | 학생 이름입니다. 후보 목록에 없는 `studentId`(출결 미입력 등)를 요청한 경우 `null`입니다. |
| `data[].sent` | 발송 성공 여부입니다. |
| `data[].failureReason` | 실패 사유입니다. 성공(`sent: true`)이면 `null`입니다. |

> 참고: 요청한 `studentIds` 각각에 대해 개별로 발송을 시도하며(학생 1명당 Solapi API 호출 1건, 배치 아님), 일부 학생이 실패해도 HTTP 상태는 `200`으로 고정되고 `data` 배열의 학생별 결과로만 성공/실패가 구분됩니다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `studentIds`가 비어있음/null (Bean Validation) |
| `400 Bad Request` | `ROLLCALL_400_2` | 발송할 학생을 최소 1명 이상 선택해야 합니다. | `studentIds`가 빈 리스트로 커맨드까지 도달한 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | `ROLLCALL:MANAGE` 권한 없음 |
| `404 Not Found` | `ROLLCALL_404_2` | 강의를 찾을 수 없습니다. | `lectureId`가 요청자 학원 소속 강의가 아니거나 존재하지 않음 |

> 참고: 학생 개별 발송 실패(출결 미입력, 매칭 템플릿 없음, Solapi API 호출 실패 등)는 HTTP 오류가 아니라 `data[].sent: false` + `failureReason`으로 표현됩니다.
