# 🧭 platform 설계 배경 (REVISION)

## 왜 새 도메인으로 분리했나

- 다른 도메인의 데이터를 조합해 보여주는 읽기 전용 대시보드이고, 담당자·변경 주기가 users/workspace 등 기존 도메인과 다르다 — [MODULES.md](../../../../../../../../docs/MODULES.md)의 "새 모듈 추가 기준"(다른 담당자/변경 주기, 독립된 공개 계약)에 해당한다.
- 상태를 저장하지 않는 순수 조회 도메인이라, [ARCHITECTURE.md](../../../../../../../../docs/ARCHITECTURE.md)의 "조회 전용 예외"(관리자 화면·통계·대시보드는 Read Model로 분리 가능) 조항을 그대로 적용했다.

## operational-metrics 응답을 Domain Model 그대로 내려주지 않은 이유

- 초기 구현에서는 Controller가 `OperationalMetrics`(domain.model)를 그대로 `GlobalApiResponse`에 담아 반환했다. `academies`/`member-count`/`storage-usage` 세 API는 이미 `Response.from(domain)` 패턴을 따르고 있었는데 이 API만 예외였다.
- ARCHITECTURE.md의 "Query Service는 Domain Model을 반환하고 Controller가 `Response.from(domain)`으로 변환한다" 규칙을 어기고 있어, `OperationalMetricsResponse`(및 중첩 `RdsConnectionBudgetResponse`/`EcsHostHeadroomResponse`, 별도 `ApiCallMetricResponse`)를 추가해 도메인 모델이 HTTP 응답에 직접 노출되지 않도록 정리했다.

## 테넌트 레지스트리를 왜 "단일 dashboard host Task"에만 주입하나

- 처음에는 배포 스크립트가 `PLATFORM_DASHBOARD_TENANT_REGISTRY_JSON`(전체 학원의 RDS 식별자·최대 커넥션·ECS 클러스터/서비스명·S3 버킷명 포함)을 **모든 학원 Task**에 동일하게 주입했다. 학원마다 별도 EC2·DB를 쓰는 이 서비스의 테넌시 모델(ARCHITECTURE.md)에서, 한 학원의 Task가 다른 모든 학원의 인프라 식별자를 컨테이너 환경변수로 갖고 있는 건 격리 원칙과 어긋난다 — 그 Task 하나가 침해되면 전체 학원의 인프라 지도가 노출된다.
- `infra/tenants.yml`에 `platform_dashboard_host: true` 필드를 추가해 정확히 한 학원만 "대시보드 호스트"로 지정하고, 배포 스크립트(`validate_capacity`)가 활성 테넌트 중 정확히 1개만 host인지 검증한다. 테넌트 레지스트리와 `platform.dashboard.enabled=true`는 그 host Task에만 주입된다.
- 조회용 Bean(Controller, QueryService, Prometheus/ECS/S3 어댑터, TenantRegistry)은 `@ConditionalOnProperty(platform.dashboard.enabled=true)`로 host Task에서만 활성화된다. 단, 각 학원이 자기 지표를 Prometheus에 노출하는 **자가 보고 컴포넌트**(`ActiveMemberGauge`/`DatabaseStorageGauge`, 그리고 이를 구현하는 `PlatformActiveMemberCountAdapter`/`CurrentTenantDatabaseUsageAdapter`)는 이 조건과 무관하게 모든 Task에서 항상 켜져 있어야 한다 — host가 아니면 자기 지표를 아무도 안 보내서 대시보드에 빈 값만 나온다.

## 왜 회원 수·데이터 보유량은 "학원별 단일 조회"만 지원하고 "전체 비교"는 없나

- 원 기능 명세는 회원 수·데이터 보유량 대시보드에 "전체 학원 비교 차트"를 요구했지만, 담당자 판단으로 이번 구현 범위는 학원 하나씩 선택해 조회하는 것으로 좁혔다. 전체 운영 지표(`operational-metrics`)는 `scope=ALL|ACADEMY`로 둘 다 지원한다는 점과 대비된다.
- 프론트에서 전체 비교가 꼭 필요하면 `academies()`로 목록을 받아 학원 수만큼 반복 호출하는 방식이 되는데, 특히 `storage-usage`는 매 호출마다 S3 `ListObjectsV2` 전체 페이지네이션(버킷 2개)을 수행하므로 학원 수가 늘면 느려질 수 있다 — 학원 수가 늘어나면 재검토가 필요하다.

## users 도메인과의 연결 (조회 Port)

- `ActiveMemberCountPort`는 platform이 정의하고, users 도메인이 `PlatformActiveMemberCountAdapter`로 구현한다([AGENTS.md](../../../../../../../../AGENTS.md)의 "타 도메인 조회는 대상 도메인 담당자 사전 동의 필요" 절차를 따름). `UserRepository`에는 `countActiveUsers()` 메서드 하나, `UserJpaRepository`에는 `countByStatus()` 파생 쿼리 하나만 추가했고 기존 users 로직은 건드리지 않았다.

## Flyway 마이그레이션이 없는 이유

- 이 기능은 새 테이블을 만들지 않는다. 학원별 DB 사용량은 `CurrentTenantDatabaseUsageAdapter`가 자기 스키마의 `information_schema.tables`를 조회해서 얻고, 회원 수는 기존 `users` 테이블을 재사용한다. 그래서 이 도메인은 `db/migration`에 파일을 추가한 적이 없다 — [DATABASE.md](../../../../../../../../docs/DATABASE.md)가 경고하는 마이그레이션 순서 역전 문제가 애초에 발생할 수 없는 구조다.

## IAM 권한은 왜 배포 스크립트가 자동으로 안 붙여주나

- 이 리포지토리의 IAM 관리는 Terraform 등 IaC로 되어있지 않고 콘솔/CLI로 수동 관리된다(기존 관행). ECS Describe 4종, S3 `ListBucket`(`tenants/*`, staff+finance 버킷) 권한을 dashboard host의 Task Role(`mudo-prod-tenant-academy-a-task-role`)에 인라인 정책(`mudo-prod-tenant-academy-a-platform-dashboard-access`)으로 2026-08-13에 수동 적용했다.
- 기존 파일 업로드/다운로드용 정책(`mudo-prod-tenant-academy-a-s3-access`)과는 별도 정책으로 분리했다 — 이 기능이 나중에 전용 서비스로 분리될 때 통째로 떼어내기 쉽게 하기 위함이다.
