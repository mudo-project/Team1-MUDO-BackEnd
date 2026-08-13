# 플랫폼 대시보드 운영 연동

## 접근과 API

- 대상은 `PLATFORM:SUPER_ADMIN` 권한을 가진 플랫폼 관리자다.
- 모든 API는 `/api/platform/**` 아래에 있으며, 일반 학원 사용자와 학원 관리자는 접근할 수 없다.
- 지원 API는 다음과 같다.
  - `GET /api/platform/academies`
  - `GET /api/platform/operational-metrics?scope=ALL|ACADEMY&academyCode={code}&period=LAST_HOUR|LAST_24_HOURS|TODAY`
  - `GET /api/platform/academies/{academyCode}/member-count`
  - `GET /api/platform/academies/{academyCode}/storage-usage`

`operational-metrics`는 전체 또는 선택 학원 기준으로 조회한다. 회원 수와 저장량은 선택한 학원 하나만 조회한다.

## 지표 출처

- 주요 API 호출 빈도, p95 응답시간, 오류율, 활성 회원 수, 학원별 DB 사용량은 Prometheus에서 조회한다.
- 각 앱 Task는 `mudo_active_members`, `mudo_database_storage_bytes` 지표를 노출한다.
- ECS 배치·호스트 여유는 ECS API에서 조회한다.
- S3 파일 사용량은 Staff/Finance 버킷의 `tenants/{academyCode}/` Prefix를 합산한다.

학원별 DB 사용량은 공유 RDS 인스턴스 전체 용량이 아니라 각 학원 DB의 테이블·인덱스 크기 합계다. 같은 RDS Cell을 공유하는 학원도 값이 분리된다.

## 배포 시 주입되는 테넌트 목록

`infra/scripts/deploy_production.py`는 `infra/tenants.yml`과 `infra/cells.yml`을 읽어 `PLATFORM_DASHBOARD_TENANT_REGISTRY_JSON`을 각 ECS 앱 Task에 주입한다. 학원을 추가한 뒤 배포하면 대시보드 학원 목록에도 자동으로 포함된다.

## ECS Task Role 최소 추가 권한

플랫폼 API를 제공하는 앱 Task Role에는 아래 읽기 권한이 필요하다.

```json
{
  "Effect": "Allow",
  "Action": [
    "ecs:ListContainerInstances",
    "ecs:DescribeContainerInstances",
    "ecs:ListTasks",
    "ecs:DescribeTasks"
  ],
  "Resource": "*"
}
```

S3 사용량 조회에는 두 버킷의 `s3:ListBucket` 권한과 `tenants/*` Prefix 조건을 부여한다. 파일 원문 조회·다운로드 권한은 이 대시보드에 필요하지 않다.

```json
{
  "Effect": "Allow",
  "Action": "s3:ListBucket",
  "Resource": [
    "arn:aws:s3:::<staff-bucket>",
    "arn:aws:s3:::<finance-bucket>"
  ],
  "Condition": {
    "StringLike": {
      "s3:prefix": ["tenants/*"]
    }
  }
}
```

현재 구조에서는 앱 Task가 S3 Prefix 크기를 집계한다. 운영 규모가 커지면 대시보드 전용 ECS 서비스와 전용 IAM Role로 분리한다.
