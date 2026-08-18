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
- 관리자 대시보드 조회용 Bean(`PlatformDashboardController`, `PlatformDashboardQueryService`, Prometheus/ECS/S3 어댑터, `PlatformTenantRegistry`)은 `@ConditionalOnProperty(platform.dashboard.enabled=true)`로 host Task에서만 활성화된다. 단, 각 학원이 자기 지표를 Prometheus에 노출하는 **자가 보고 컴포넌트**(`ActiveMemberGauge`/`DatabaseStorageGauge`, 그리고 이를 구현하는 `PlatformActiveMemberCountAdapter`/`CurrentTenantDatabaseUsageAdapter`)는 이 조건과 무관하게 모든 Task에서 항상 켜져 있어야 한다 — host가 아니면 자기 지표를 아무도 안 보내서 대시보드에 빈 값만 나온다.

## 왜 테넌트 라우팅 디렉터리(`/api/public/tenants/{code}`)는 dashboard host 조건에서 뺐나

- 처음 구현할 때 `TenantDirectoryController`/`TenantDirectoryQueryService`/`PlatformTenantDirectory`(그리고 이 셋이 의존하는 `PlatformDashboardProperties` 빈을 등록하는 `PlatformDashboardConfiguration`)를 위 관리자 대시보드 Bean들과 같은 `@ConditionalOnProperty(platform.dashboard.enabled=true)`에 묶어버렸다. 이 API는 프론트가 로그인 전에 학원 코드→실제 API 호스트를 조회하는 **공개 라우팅 API**로, 관리자 대시보드 기능과는 목적이 다르다.
- 2026-08-19에 `platform_dashboard_host`를 academy-a에서 academy-d로 옮기면서 이 결합이 실제 장애로 드러났다. 프론트의 `TENANT_ROUTING_ORIGIN`은 학원마다 바뀌는 값이 아니라 이 조회 API를 부르는 고정 진입점(academy-a)인데, dashboard host가 아닌 Task에서는 컨트롤러 자체가 사라지면서 academy-a가 이 API에 500을 반환했다. 그 결과 `app-<code>.ieum.store` 서브도메인을 쓰는 모든 학원의 로그인 라우팅이 함께 막혔다(root 도메인은 이 조회를 안 해서 무관).
- 따라서 이 세 Bean(및 `PlatformDashboardConfiguration`)의 `@ConditionalOnProperty`를 제거해 **모든 활성 테넌트 Task에서 항상 등록**되도록 바꿨고, `infra/scripts/deploy_production.py`도 `PLATFORM_TENANT_DIRECTORY_JSON`을 dashboard host 여부와 무관하게 모든 활성 테넌트에 주입하도록 고쳤다. dashboard host 전용으로 남겨야 하는 관리자 레지스트리(`PLATFORM_DASHBOARD_TENANT_REGISTRY_JSON`, ECS/RDS 내부 식별자 포함)는 격리 원칙 그대로 유지했다 — 공개 라우팅 정보와 관리자 내부 정보는 민감도가 달라 같은 조건에 묶으면 안 된다.

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

## PlatformTenantRegistry 캐싱과 tenantMatcher 통일 (2026-08-13)

- `findAll()`이 요청마다(그리고 `get()`이 내부에서 `findAll()`을 다시 호출해 한 요청 안에서도 여러 번) 정적 JSON을 재파싱하고 있었다. 배포 시점에 고정되는 값이라 다시 파싱할 이유가 없어, `volatile` 필드 + double-checked locking으로 최초 1회만 파싱하도록 바꿨다. `@PostConstruct`로 미리 파싱하지 않고 지연 초기화(lazy)로 한 이유는, Spring 컨테이너 없이 단위 테스트에서 직접 `new`로 생성해도 동작해야 했기 때문이다.
- `PrometheusOperationalMetricsAdapter.tenantMatcher`가 학원 목록이 비었을 때 다른 Prometheus 어댑터(`.*` fallback)와 다르게 빈 문자열을 그대로 써서 `tenant=~""`(항상 0 매칭)이 되는 불일치가 있었다. 세 어댑터 모두 `.*` fallback으로 통일했다.

## operational-metrics 외부 호출 병렬화 (2026-08-13)

- `operationalMetrics()` 한 번 호출에 Prometheus 쿼리 11회(카테고리 8→11개로 늘어난 뒤로는 `apiCallMetrics` 내부만 11회) + p95/오류율/커넥션 수 3회 + ECS API 호출이 전부 순차로 나가 눈에 띄게 느렸다.
- `PlatformDashboardQueryService.operationalMetrics()`의 5개 독립 Port 호출(activeDatabaseConnections/apiCallMetrics/p95/errorRate/ecsHeadrooms)과 `PrometheusOperationalMetricsAdapter.apiCallMetrics()`의 카테고리별 호출을 각각 `CompletableFuture.supplyAsync(..., executor)`로 병렬화했다. Port 인터페이스 시그니처는 그대로 유지했다 — 병렬화는 호출부(QueryService, Adapter 내부)에서만 일어나고 각 Port 메서드 자체는 여전히 동기 메서드다.
- Executor는 새로 만들지 않고 `global.infrastructure.executor.AsyncExecutionConfig`가 이미 제공하는 `applicationTaskExecutor` 빈을 재사용했다.
- **Lombok 함정**: 처음에는 `@RequiredArgsConstructor`를 쓰는 클래스에서 `Executor` 필드에 `@Qualifier("applicationTaskExecutor")`만 붙이면 될 거라 가정했는데, 실제로는 이 프로젝트의 Lombok 설정에서 필드의 `@Qualifier`가 생성자 파라미터로 복사되지 않아 `NoUniqueBeanDefinitionException`(웹소켓 채널용 Executor 빈 3개와 충돌)이 났다. `PlatformDashboardQueryService`와 `PrometheusOperationalMetricsAdapter` 둘 다 `@RequiredArgsConstructor`를 떼고 생성자를 직접 작성해 `@Qualifier`를 파라미터에 명시적으로 붙이는 방식으로 고쳤다. `@SpringBootTest` 기반 통합 테스트(`PlatformDashboardControllerPermissionIntegrationTest`)가 아니었다면 이 문제는 순수 Mockito 단위 테스트만으로는 못 잡았을 것이다.

## api-call-frequency 엔드포인트를 별도로 추가한 이유 (2026-08-13)

- `operational-metrics.apiCallMetrics`는 기능 명세대로 "학원별 비교·필터를 제공하지 않는 전체 서비스 합산" 지표로 유지해야 했다. 하지만 실제로는 학원별 요청량을 비교하고 싶다는 요구가 생겨서, 기존 엔드포인트의 의미를 바꾸는 대신 `GET /api/platform/api-call-frequency`를 새로 만들었다 — API_CONTRACT.md의 "기존 응답 필드는 제거·변경하지 않는다" 원칙을 지키기 위함이다.
- 단순히 학원마다 `scope=ACADEMY`로 반복 호출하게 하면 학원 수만큼 API 호출이 늘어나는 N+1 문제가 생긴다(`member-count`/`storage-usage`가 이미 이 패턴이다). 대신 PromQL을 `sum(...)`에서 `sum by (tenant) (...)`로 바꿔, 카테고리 하나당 쿼리 1번으로 그 카테고리의 전체 학원 값을 동시에 받아온다 — 쿼리 횟수가 카테고리 수(11개)로 고정되고 학원 수와 무관해진다.
- 집계 기간 동안 호출이 전혀 없는 학원은 Prometheus 응답(`result[]`)에 아예 나타나지 않는다. `PlatformDashboardQueryService.apiCallFrequency()`가 조회 대상 학원 목록을 기준으로 `byAcademy.getOrDefault(academyCode, List.of())`로 매핑해, 그런 학원도 빈 목록으로 응답에 포함되도록 보정했다 — 그렇지 않으면 프론트 비교 화면에서 그 학원이 통째로 빠져 보인다.
- `ApiCallFrequencyPort`를 기존 `OperationalMetricsPort`에 메서드를 추가하는 대신 별도 Port로 분리했다 — 이 코드베이스는 Port 하나당 관심사 하나(`MemberCountMetricsPort`, `StorageUsagePort`처럼) 원칙을 따르고 있어서, 구현체(`PrometheusOperationalMetricsAdapter`)는 같아도 계약은 나눴다.

## apiCallMetrics 카테고리가 8개에서 11개로 세분화됨

- 최초 구현 시점에는 `WORKSPACE_TASK_MUTATION`(업무 등록+상태 변경 통합), `APPROVAL_SETTLEMENT_SUBMISSION`(결재+정산 통합), `CALENDAR_MEMO_CREATE`(일정+메모 통합) 등 8개 카테고리였는데, 이후 `WORKSPACE_TASK_CREATE`/`WORKSPACE_TASK_STATUS_CHANGE`, `APPROVAL_SUBMISSION`/`SETTLEMENT_SUBMISSION`, `CALENDAR_CREATE`/`MEMO_CREATE`로 각각 더 세분화되어 11개가 됐다(출근 체크 카테고리명도 `ATTENDANCE_CHECK_IN_OUT`에서 `CHECK_IN`으로 바뀌었다). 세분화 배경(왜 나눴는지)은 커밋 히스토리에 별도로 남아있지 않아 여기 기록하지 못한다 — 다만 문서(API.md/API_FLOW.md)는 항상 실제 코드의 `PrometheusOperationalMetricsAdapter.categories()`를 기준으로 맞춰야 한다.
