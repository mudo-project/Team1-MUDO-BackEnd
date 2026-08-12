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
    "apiCallMetrics": [{ "category": "ACCOUNT_ISSUANCE", "count": 3 }],
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

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `PLATFORM_400_1` | 학원 코드가 필요합니다. | `scope=ACADEMY`인데 `academyCode`가 없거나 빈 값 |
| `404 Not Found` | `PLATFORM_404_1` | 조회할 학원을 찾을 수 없습니다. | `academyCode`가 테넌트 레지스트리에 없음 |
| `503 Service Unavailable` | `PLATFORM_503_1` | 운영 지표를 현재 조회할 수 없습니다. | Prometheus/ECS 조회 실패 |

비즈니스 규칙: 같은 RDS Cell(`rdsIdentifier`)을 공유하는 학원의 `rdsConnectionBudget.safeBudget`은 Cell 단위로 중복 없이 한 번만 합산한다.

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
