# platform API 명세

Controller, Request/Response DTO, Security 설정, 예외 코드 구현을 기준으로 작성했다.

## 공통

- Header: `Authorization: Bearer {accessToken}`
- 필요 권한: `PLATFORM:SUPER_ADMIN` (권한 카탈로그에 없는 합성 authority. `account_type=ADMIN`+`admin_scope=PLATFORM` 계정에만 `JwtAuthenticationConverter`가 부여하며, 학원 관리자가 자기 역할에 배정할 수 없다)
- 이 Controller는 `platform.dashboard.enabled=true`인 Task(dashboard host)에서만 Bean이 등록된다. 그 외 Task에서는 라우트 자체가 존재하지 않아 `404`가 반환된다.
- 공통 성공 응답 형식과 오류 응답 형식은 [API_CONTRACT.md](../../../../../../../../docs/API_CONTRACT.md) 참고.

## GET /api/platform/academies

배포된 학원(테넌트) 목록을 조회한다.

Request: 없음

성공 코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 학원 목록 조회 성공 |

Response Body

```json
{
  "status": 200,
  "code": "PLATFORM_200_1",
  "message": "플랫폼 학원 목록 조회에 성공했습니다.",
  "data": [
    { "code": "academy-a" }
  ]
}
```

비즈니스 규칙: 배포 시 `infra/tenants.yml` 기준으로 생성된 테넌트 레지스트리를 학원 코드 오름차순으로 반환한다.

## GET /api/platform/operational-metrics

전체 또는 선택 학원의 운영 성능·자원 지표를 조회한다.

Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `scope` | `ALL` \| `ACADEMY` | false (기본 `ALL`) | 전체 학원 합산 또는 특정 학원 단위 조회 |
| `academyCode` | `String` | `scope=ACADEMY`일 때 필수 | 조회 대상 학원 코드 |
| `period` | `LAST_HOUR` \| `LAST_24_HOURS` \| `TODAY` | false (기본 `LAST_HOUR`) | 집계 기간 |

성공 코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 운영 지표 조회 성공 |

Response Body

```json
{
  "status": 200,
  "code": "PLATFORM_200_2",
  "message": "운영 성능·자원 지표 조회에 성공했습니다.",
  "data": {
    "scope": "ALL",
    "academyCode": null,
    "period": "LAST_HOUR",
    "apiCallMetrics": [
      { "category": "INITIAL_DATA_READ", "count": 128 },
      { "category": "ACCOUNT_ISSUANCE", "count": 3 },
      { "category": "CHECK_IN", "count": 45 },
      { "category": "ATTENDANCE_EXPORT", "count": 2 },
      { "category": "NOTICE_CREATE", "count": 4 },
      { "category": "WORKSPACE_TASK_CREATE", "count": 17 },
      { "category": "WORKSPACE_TASK_STATUS_CHANGE", "count": 22 },
      { "category": "APPROVAL_SUBMISSION", "count": 6 },
      { "category": "SETTLEMENT_SUBMISSION", "count": 1 },
      { "category": "CALENDAR_CREATE", "count": 5 },
      { "category": "MEMO_CREATE", "count": 9 }
    ],
    "p95ResponseMilliseconds": 120.5,
    "errorRatePercent": 1.2,
    "rdsConnectionBudget": { "current": 10, "safeBudget": 100, "usedPercent": 10.0 },
    "ecsHostHeadrooms": [
      {
        "cluster": "mudo-prod-cluster",
        "hostId": "i-1",
        "registeredCpu": 2048,
        "registeredMemoryMib": 1913,
        "remainingCpu": 1024,
        "remainingMemoryMib": 900,
        "academyCodes": ["academy-a"]
      }
    ]
  }
}
```

`apiCallMetrics`는 위 예시처럼 11개 카테고리(`INITIAL_DATA_READ`, `ACCOUNT_ISSUANCE`, `CHECK_IN`, `ATTENDANCE_EXPORT`, `NOTICE_CREATE`, `WORKSPACE_TASK_CREATE`, `WORKSPACE_TASK_STATUS_CHANGE`, `APPROVAL_SUBMISSION`, `SETTLEMENT_SUBMISSION`, `CALENDAR_CREATE`, `MEMO_CREATE`)를 항상 전부 반환한다(활동이 없으면 `count: 0`). 각 카테고리는 `메서드 + 경로 패턴`에 매칭되는 실제 업무 액션 발생 횟수다 — 예를 들어 `CHECK_IN`은 `POST /api/attendance/check-ins`(출근 체크인), `APPROVAL_SUBMISSION`은 `POST /api/approvals`(결재 상신) 호출 수를 센다. 서버 부하 지표가 아니라 업무 활동 지표이며, `scope`와 무관하게 항상 전체 서비스 합산이다(위 "비즈니스 규칙" 참고).

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `PLATFORM_400_1` | 학원 코드가 필요합니다. | `scope=ACADEMY`인데 `academyCode`가 없거나 빈 값 |
| `404 Not Found` | `PLATFORM_404_1` | 조회할 학원을 찾을 수 없습니다. | `academyCode`가 테넌트 레지스트리에 없음 |
| `503 Service Unavailable` | `PLATFORM_503_1` | 운영 지표를 현재 조회할 수 없습니다. | Prometheus/ECS 조회 실패 |

비즈니스 규칙:
- 같은 RDS Cell(`rdsIdentifier`)을 공유하는 학원의 `rdsConnectionBudget.safeBudget`은 Cell 단위로 중복 없이 한 번만 합산한다.
- `apiCallMetrics`는 `scope` 값과 무관하게 **항상 전체 서비스 합산**이다(기능 명세상 이 지표는 학원별 비교·필터를 제공하지 않는다). `scope=ACADEMY`를 줘도 `apiCallMetrics`만은 전체 학원 기준으로 나온다 — 학원별로 보려면 아래 `api-call-frequency`를 쓴다.
- `p95ResponseMilliseconds`/`errorRatePercent`/`rdsConnectionBudget`/`ecsHostHeadrooms`는 `scope`에 맞춰 스코핑된다.

## GET /api/platform/api-call-frequency

전체 또는 선택 학원의 주요 업무 API 호출 빈도를 **학원별로 나란히 비교**해서 조회한다. `operational-metrics.apiCallMetrics`(항상 전체 합산)와 달리 이 API는 학원별 breakdown을 제공한다.

Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `scope` | `ALL` \| `ACADEMY` | false (기본 `ALL`) | 전체 학원 또는 특정 학원 단위 조회 |
| `academyCode` | `String` | `scope=ACADEMY`일 때 필수 | 조회 대상 학원 코드 |
| `period` | `LAST_HOUR` \| `LAST_24_HOURS` \| `TODAY` | false (기본 `LAST_HOUR`) | 집계 기간 |

성공 코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 학원별 API 호출 빈도 조회 성공 |

Response Body

```json
{
  "status": 200,
  "code": "PLATFORM_200_5",
  "message": "학원별 API 호출 빈도 조회에 성공했습니다.",
  "data": [
    {
      "academyCode": "academy-a",
      "apiCallMetrics": [
        { "category": "INITIAL_DATA_READ", "count": 84 },
        { "category": "ACCOUNT_ISSUANCE", "count": 3 },
        { "category": "CHECK_IN", "count": 30 },
        { "category": "ATTENDANCE_EXPORT", "count": 0 },
        { "category": "NOTICE_CREATE", "count": 0 },
        { "category": "WORKSPACE_TASK_CREATE", "count": 0 },
        { "category": "WORKSPACE_TASK_STATUS_CHANGE", "count": 12 },
        { "category": "APPROVAL_SUBMISSION", "count": 4 },
        { "category": "SETTLEMENT_SUBMISSION", "count": 0 },
        { "category": "CALENDAR_CREATE", "count": 0 },
        { "category": "MEMO_CREATE", "count": 0 }
      ]
    },
    {
      "academyCode": "academy-b",
      "apiCallMetrics": [
        { "category": "INITIAL_DATA_READ", "count": 0 },
        { "category": "ACCOUNT_ISSUANCE", "count": 0 },
        { "category": "CHECK_IN", "count": 0 },
        { "category": "ATTENDANCE_EXPORT", "count": 0 },
        { "category": "NOTICE_CREATE", "count": 0 },
        { "category": "WORKSPACE_TASK_CREATE", "count": 0 },
        { "category": "WORKSPACE_TASK_STATUS_CHANGE", "count": 0 },
        { "category": "APPROVAL_SUBMISSION", "count": 0 },
        { "category": "SETTLEMENT_SUBMISSION", "count": 0 },
        { "category": "CALENDAR_CREATE", "count": 0 },
        { "category": "MEMO_CREATE", "count": 0 }
      ]
    }
  ]
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `PLATFORM_400_1` | 학원 코드가 필요합니다. | `scope=ACADEMY`인데 `academyCode`가 없거나 빈 값 |
| `404 Not Found` | `PLATFORM_404_1` | 조회할 학원을 찾을 수 없습니다. | `academyCode`가 테넌트 레지스트리에 없음 |
| `503 Service Unavailable` | `PLATFORM_503_1` | 운영 지표를 현재 조회할 수 없습니다. | Prometheus 조회 실패 |

비즈니스 규칙:
- `scope=ALL`이면 조회 대상 학원 전체가 항상 포함된다 — 집계 기간 동안 호출이 전혀 없었던 학원도 목록에서 누락되지 않는다.
- 각 학원의 `apiCallMetrics`는 **항상 11개 카테고리 전부**를 포함한다(활동이 없는 카테고리는 `count: 0`). `operational-metrics.apiCallMetrics`와 동일한 규칙이라, 클라이언트가 "배열에 없으면 0"을 직접 처리할 필요가 없다.
- PromQL `sum by (tenant)`로 카테고리당 1번의 쿼리로 전체 학원의 값을 한 번에 받아오므로, 학원 수가 늘어도 쿼리 횟수는 카테고리 수(11개)로 고정된다.

## GET /api/platform/academies/{academyCode}/member-count

선택한 학원 하나의 현재 활성 회원 수를 조회한다. 학원 간 비교는 지원하지 않는다.

Path Variable

| name | description |
| --- | --- |
| `academyCode` | 조회 대상 학원 코드 |

성공 코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 회원 수 조회 성공 |

Response Body

```json
{
  "status": 200,
  "code": "PLATFORM_200_3",
  "message": "학원 회원 수 조회에 성공했습니다.",
  "data": {
    "academyCode": "academy-a",
    "activeMemberCount": 12,
    "collectedAt": "2026-08-13T12:00:00+09:00"
  }
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `404 Not Found` | `PLATFORM_404_1` | 조회할 학원을 찾을 수 없습니다. | `academyCode`가 테넌트 레지스트리에 없음 |
| `503 Service Unavailable` | `PLATFORM_503_1` | 운영 지표를 현재 조회할 수 없습니다. | Prometheus 조회 실패 |

## GET /api/platform/academies/{academyCode}/storage-usage

선택한 학원 하나의 DB·S3 저장량을 조회한다. 학원 간 비교는 지원하지 않는다.

Path Variable

| name | description |
| --- | --- |
| `academyCode` | 조회 대상 학원 코드 |

성공 코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 데이터 보유량 조회 성공 |

Response Body

```json
{
  "status": 200,
  "code": "PLATFORM_200_4",
  "message": "학원 데이터 보유량 조회에 성공했습니다.",
  "data": {
    "academyCode": "academy-a",
    "databaseBytes": 10485760,
    "s3Bytes": 52428800,
    "collectedAt": "2026-08-13T12:00:00+09:00"
  }
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `404 Not Found` | `PLATFORM_404_1` | 조회할 학원을 찾을 수 없습니다. | `academyCode`가 테넌트 레지스트리에 없음 |
| `503 Service Unavailable` | `PLATFORM_503_1` | 운영 지표를 현재 조회할 수 없습니다. | S3/DB 조회 실패 |

비즈니스 규칙: `databaseBytes`는 학원 자신의 RDS 스키마(`information_schema`) 기준이며, 같은 RDS Cell을 공유하는 다른 학원 용량과 섞이지 않는다. `s3Bytes`는 staff·finance 버킷의 `tenants/{academyCode}/` Prefix 합산이다(파일 원문·다운로드 주소는 포함하지 않는다).
