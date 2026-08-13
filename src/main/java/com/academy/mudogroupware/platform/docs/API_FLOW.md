# 🔄 platform 처리 Flow

## 1. 학원 목록 조회 흐름

```text
PLATFORM:SUPER_ADMIN 인증된 요청
→ PlatformDashboardController.academies
→ PlatformDashboardQueryService.academies()
→ PlatformTenantRegistry.findAll()
   → platform.dashboard.tenant-registry-json(배포 시 주입된 JSON)을 Jackson으로 파싱
   → 학원 코드 오름차순 정렬
→ AcademyResponse.from(...) 목록
→ GlobalApiResponse<List<AcademyResponse>>
```

- 외부 API 호출이 전혀 없다 — 배포 시 주입된 정적 JSON만 읽는다.

## 2. 운영 성능·자원 지표 조회 흐름

```text
PLATFORM:SUPER_ADMIN 인증된 요청
→ PlatformDashboardController.operationalMetrics(scope, academyCode, period)
→ PlatformDashboardQueryService.operationalMetrics
→ allAcademies = PlatformTenantRegistry.findAll()  (apiCallMetrics·ecsHeadrooms 원본 조회용, scope 무관 항상 전체)
→ academies = select(scope, academyCode)            (activeDatabaseConnections·p95·errorRate·safeBudget용)
   → scope=ALL: PlatformTenantRegistry.findAll()
   → scope=ACADEMY: academyCode 없으면 PlatformException(ACADEMY_CODE_REQUIRED)
                     있으면 PlatformTenantRegistry.get(academyCode) (없으면 ACADEMY_NOT_FOUND)
→ 아래 5개를 CompletableFuture.supplyAsync(..., applicationTaskExecutor)로 동시에 시작
   → OperationalMetricsPort.activeDatabaseConnections(academies)
   → OperationalMetricsPort.apiCallMetrics(allAcademies, period)   ※ scope 무관, 항상 전체 합산
   → OperationalMetricsPort.p95ResponseMilliseconds(academies, period)
   → OperationalMetricsPort.errorRatePercent(academies, period)
   → EcsHeadroomPort.findHeadrooms(allAcademies)
      ※ PrometheusOperationalMetricsAdapter/AwsEcsHeadroomAdapter가 각각 구현
→ safeBudget은 로컬 계산(외부 호출 없음) — 같은 RDS Cell(rdsIdentifier)을 공유하는 학원을 묶어 Cell당 한 번만 합산
→ 각 Future를 join()해서 결과 취합, ecsHeadrooms는 scope=ACADEMY면 academyCode가 포함된 것만 필터링
→ OperationalMetrics(도메인 모델) 조립
→ OperationalMetricsResponse.from(...)
→ GlobalApiResponse<OperationalMetricsResponse>
```

- Prometheus·ECS 호출 실패는 모두 `PlatformException(METRICS_UNAVAILABLE, 503)`로 변환된다 — 기술 예외를 그대로 노출하지 않는다.
- `apiCallMetrics`는 `scope`와 무관하게 항상 `allAcademies` 기준이다 — 기능 명세상 이 지표는 학원별 필터를 제공하지 않기 때문이다. 학원별로 보려면 3번(api-call-frequency) 참고.
- `PrometheusOperationalMetricsAdapter.apiCallMetrics()` 내부에서도 11개 카테고리를 순차가 아니라 `CompletableFuture`로 병렬 조회한다.

## 3. 학원별 API 호출 빈도 비교 조회 흐름

```text
PLATFORM:SUPER_ADMIN 인증된 요청
→ PlatformDashboardController.apiCallFrequency(scope, academyCode, period)
→ PlatformDashboardQueryService.apiCallFrequency
→ academies = select(scope, academyCode)  (2번과 동일한 검증 규칙)
→ ApiCallFrequencyPort.apiCallMetricsByAcademy(academies, period)
   ※ PrometheusOperationalMetricsAdapter가 구현
   → 카테고리 11개를 병렬로 조회하되, 각 쿼리는 sum by (tenant) (...)로
     해당 카테고리의 전체 학원 값을 한 번에 받아온다(학원 수만큼 반복 호출하지 않음)
   → Prometheus 응답의 result[].metric.tenant를 키로 Map<학원코드, List<ApiCallMetric>>로 재구성
→ academies 각각에 대해 byAcademy.getOrDefault(academyCode, List.of())로 매핑
   ※ 집계 기간 동안 호출이 전혀 없어 Prometheus 응답에 아예 안 나온 학원도 빈 목록으로 채워 넣는다
→ List<AcademyApiCallMetrics>(도메인 모델) 조립
→ AcademyApiCallFrequencyResponse::from 목록
→ GlobalApiResponse<List<AcademyApiCallFrequencyResponse>>
```

## 4. 학원 회원 수 조회 흐름

```text
PLATFORM:SUPER_ADMIN 인증된 요청
→ PlatformDashboardController.memberCount(academyCode)
→ PlatformDashboardQueryService.activeMemberCount(academyCode)
→ PlatformTenantRegistry.get(academyCode) (없으면 ACADEMY_NOT_FOUND)
→ MemberCountMetricsPort.activeMemberCount([academyCode])
   ※ PrometheusMemberCountMetricsAdapter가 구현 — sum(mudo_active_members{tenant=~"academyCode"}) 질의
→ MemberCountResponse(academyCode, count, Instant.now())
→ GlobalApiResponse<MemberCountResponse>
```

- `mudo_active_members` 지표는 각 학원 앱 Task가 스스로 노출한다(아래 6번 "자가 보고 흐름" 참고) — 이 API는 Prometheus에 이미 쌓인 값을 읽기만 한다.

## 5. 학원 데이터 보유량 조회 흐름

```text
PLATFORM:SUPER_ADMIN 인증된 요청
→ PlatformDashboardController.storageUsage(academyCode)
→ PlatformDashboardQueryService.storageUsage(academyCode)
→ PlatformTenantRegistry.get(academyCode) (없으면 ACADEMY_NOT_FOUND)
→ DatabaseUsageMetricsPort.databaseBytes([academyCode])
   ※ PrometheusDatabaseUsageMetricsAdapter가 구현 — sum(mudo_database_storage_bytes{tenant=~"academyCode"}) 질의
→ StorageUsagePort.s3Bytes(academy)
   ※ AwsStorageUsageAdapter가 구현 — staff·finance 버킷의 tenants/{academyCode}/ Prefix를 S3 ListObjectsV2로 페이지네이션 순회해 합산
→ StorageUsage(도메인 모델) 조립
→ StorageUsageResponse.from(...)
→ GlobalApiResponse<StorageUsageResponse>
```

## 6. 자가 보고 흐름 (모든 테넌트 Task에서 항상 실행 — dashboard host 여부와 무관)

```text
앱 Task 기동 시
→ ActiveMemberGauge.register() / DatabaseStorageGauge.register() (@PostConstruct)
→ Micrometer Gauge 등록
   → ActiveMemberCountPort.countActiveMembers()
      ※ users 도메인의 PlatformActiveMemberCountAdapter가 구현 — UserRepository.countActiveUsers() 호출
   → CurrentTenantDatabaseUsagePort.databaseBytes()
      ※ platform 자신의 CurrentTenantDatabaseUsageAdapter가 구현 — information_schema.tables에서 자기 스키마 합산 조회
→ Prometheus가 /actuator/prometheus를 스크레이프하며 mudo_active_members, mudo_database_storage_bytes를 tenant 라벨과 함께 수집
```

- 이 흐름은 `platform.dashboard.enabled` 조건과 무관하다. dashboard host가 아닌 Task도 자기 지표는 항상 노출해야, dashboard host가 Prometheus로 전체 학원을 집계할 수 있다.

---

## 📝 문서 정보

- 업데이트일: `2026-08-13`
- 변경 사항(요약):
  - 최초 작성 (플랫폼 운영 대시보드 기능 최초 구현, 4개 조회 API + 자가 보고 흐름).
  - `api-call-frequency`(학원별 API 호출 빈도 비교) 흐름 추가.
  - `operational-metrics`의 외부 호출 병렬화 반영, `apiCallMetrics`가 scope 무관 항상 전체 합산임을 명시.
  - `apiCallMetrics` 카테고리를 실제 코드 기준 11개로 수정(최초 작성 시 8개로 잘못 기재했던 것을 바로잡음).
