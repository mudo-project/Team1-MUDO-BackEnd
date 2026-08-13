# platform 모듈

플랫폼 관리자(`PLATFORM:SUPER_ADMIN`)를 위한 운영 대시보드 조회 기능을 담당한다. 상태를 변경하는 기능은 없다.

## 책임과 범위

- 전체/학원별 운영 지표(주요 API 호출 빈도, p95 응답시간, 오류율, RDS 커넥션 예산, ECS 호스트 CPU·메모리 여유) 조회
- 학원별 활성 회원 수, DB·S3 저장량 조회
- 배포된 학원(테넌트) 목록 조회
- 데이터 저장·변경은 하지 않는다. 오직 읽기 전용 조회만 담당한다.

## 담당자

플랫폼 대시보드 전담 (users 등 타 도메인과 무관하게 이 모듈 단독 소유)

## 소유하는 주요 데이터와 상태

- 이 모듈은 자체 DB 테이블을 소유하지 않는다.
- 유일한 상태 소스는 배포 시 주입되는 `platform.dashboard.tenant-registry-json`(`PlatformTenantRegistry`)이며, 나머지는 모두 외부 시스템(Prometheus, ECS API, S3, 자기 자신의 RDS)에서 실시간 조회한다.

## 외부에 공개하는 Application API

- `GET /api/platform/academies`
- `GET /api/platform/operational-metrics?scope=ALL|ACADEMY&academyCode={code}&period=LAST_HOUR|LAST_24_HOURS|TODAY`
- `GET /api/platform/api-call-frequency?scope=ALL|ACADEMY&academyCode={code}&period=LAST_HOUR|LAST_24_HOURS|TODAY`
- `GET /api/platform/academies/{academyCode}/member-count`
- `GET /api/platform/academies/{academyCode}/storage-usage`

세부 요청·응답·오류 코드는 [API.md](API.md) 참고.

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **users 모듈**: `ActiveMemberCountPort`(platform이 정의) → `PlatformActiveMemberCountAdapter`(users가 구현, 사전 협의됨). 현재 테넌트 DB의 활성 회원 수만 읽기 전용으로 제공한다.
- **Prometheus**: `mudo_active_members`, `mudo_database_storage_bytes`, `http_server_requests_seconds_*`, `hikaricp_connections_active` 지표를 `tenant` 라벨로 질의한다. 학원별 breakdown이 필요한 조회는 `sum by (tenant)`로 한 번에 받아온다.
- **AWS ECS API**: `ListContainerInstances`/`DescribeContainerInstances`/`ListTasks`/`DescribeTasks`.
- **AWS S3**: staff·finance 버킷의 `tenants/{academyCode}/` Prefix에 대한 `ListBucket`.
- **자기 자신의 RDS**(`information_schema`): 각 테넌트 Task가 자기 DB 용량을 Prometheus Gauge로 노출하기 위한 자가 조회.

## 발행·소비하는 Event

없음. 조회 전용 모듈이다.

## 변경 시 주의 사항

- 조회용 Bean(Controller, `PlatformDashboardQueryService`, Prometheus/ECS/S3 어댑터, `PlatformTenantRegistry`)은 `platform.dashboard.enabled=true`인 **단일 "dashboard host" Task**에서만 활성화된다(`@ConditionalOnProperty`). 어느 테넌트가 host인지는 `infra/tenants.yml`의 `platform_dashboard_host: true`로 지정하며, 배포 스크립트가 정확히 1개 테넌트만 host로 지정됐는지 검증한다(`infra/scripts/deploy_production.py`의 `validate_capacity`).
- **자가 보고용 컴포넌트는 예외다.** `ActiveMemberGauge`/`DatabaseStorageGauge`(및 이를 구현하는 users의 `PlatformActiveMemberCountAdapter`, platform의 `CurrentTenantDatabaseUsageAdapter`)는 `ConditionalOnProperty` 없이 **모든 테넌트 Task**에서 항상 동작해야 한다 — 각 학원이 자기 지표를 Prometheus에 노출해야 dashboard host가 이를 집계할 수 있다. 이 부분에 조건을 걸면 안 된다.
- 새 학원을 추가하고 배포하면 `PLATFORM_DASHBOARD_TENANT_REGISTRY_JSON`(`infra/scripts/deploy_production.py`의 `render_platform_tenant_registry`)이 자동으로 갱신되어 dashboard host Task에만 주입된다. `AcademyRuntime`은 실제로 사용하는 필드만 유지한다(미사용 필드를 추가하지 않는다).
- ECS Describe·S3 ListBucket에 필요한 IAM 권한은 dashboard host의 Task Role에 수동으로 부여해야 한다 — 배포 스크립트가 자동으로 붙여주지 않는다. 상세 정책과 운영 배경은 [PLATFORM_DASHBOARD_RUNTIME.md](../../../../../../../../docs/PLATFORM_DASHBOARD_RUNTIME.md) 참고.
- `operational-metrics`/`api-call-frequency`의 외부 호출은 `global.infrastructure.executor.AsyncExecutionConfig`가 제공하는 공유 `applicationTaskExecutor` 빈으로 병렬 실행한다. 이 빈은 웹소켓 채널용 Executor 빈들과 이름이 겹치지 않게 `@Qualifier("applicationTaskExecutor")`로 명시해야 한다 — Lombok `@RequiredArgsConstructor`는 필드의 `@Qualifier`를 생성자 파라미터로 복사해주지 않으므로, 이 두 클래스는 생성자를 직접 작성했다.

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
