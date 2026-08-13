package com.academy.mudogroupware.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.platform.application.port.ApiCallFrequencyPort;
import com.academy.mudogroupware.platform.application.port.DatabaseUsageMetricsPort;
import com.academy.mudogroupware.platform.application.port.EcsHeadroomPort;
import com.academy.mudogroupware.platform.application.port.MemberCountMetricsPort;
import com.academy.mudogroupware.platform.application.port.OperationalMetricsPort;
import com.academy.mudogroupware.platform.application.port.StorageUsagePort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyApiCallMetrics;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.domain.model.DashboardScope;
import com.academy.mudogroupware.platform.domain.model.StorageUsage;
import com.academy.mudogroupware.platform.infrastructure.PlatformTenantRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlatformDashboardQueryServiceTest {
  private final PlatformTenantRegistry tenantRegistry = mock(PlatformTenantRegistry.class);
  private final OperationalMetricsPort operationalMetricsPort = mock(OperationalMetricsPort.class);
  private final MemberCountMetricsPort memberCountMetricsPort = mock(MemberCountMetricsPort.class);
  private final StorageUsagePort storageUsagePort = mock(StorageUsagePort.class);
  private final DatabaseUsageMetricsPort databaseUsageMetricsPort = mock(DatabaseUsageMetricsPort.class);
  private final EcsHeadroomPort ecsHeadroomPort = mock(EcsHeadroomPort.class);
  private final ApiCallFrequencyPort apiCallFrequencyPort = mock(ApiCallFrequencyPort.class);

  private PlatformDashboardQueryService service;

  @BeforeEach
  void setUp() {
    service = new PlatformDashboardQueryService(
        tenantRegistry,
        operationalMetricsPort,
        memberCountMetricsPort,
        storageUsagePort,
        databaseUsageMetricsPort,
        ecsHeadroomPort,
        apiCallFrequencyPort,
        Runnable::run);
  }

  @Test
  void apiCallFrequencyIncludesEveryAcademyEvenWithoutData() {
    AcademyRuntime academyA = academy("academy-a");
    AcademyRuntime academyB = academy("academy-b");
    when(tenantRegistry.findAll()).thenReturn(List.of(academyA, academyB));
    when(apiCallFrequencyPort.apiCallMetricsByAcademy(List.of(academyA, academyB), DashboardPeriod.LAST_HOUR))
        .thenReturn(Map.of("academy-a", List.of(new ApiCallMetric("ACCOUNT_ISSUANCE", 3L))));

    List<AcademyApiCallMetrics> result =
        service.apiCallFrequency(DashboardScope.ALL, null, DashboardPeriod.LAST_HOUR);

    assertThat(result).extracting(AcademyApiCallMetrics::academyCode)
        .containsExactly("academy-a", "academy-b");
    assertThat(result.get(0).apiCallMetrics()).containsExactly(new ApiCallMetric("ACCOUNT_ISSUANCE", 3L));
    assertThat(result.get(1).apiCallMetrics()).isEmpty();
  }

  @Test
  void operationalMetricsRunsPortCallsConcurrentlyNotSequentially() {
    AcademyRuntime academyA = academy("academy-a");
    when(tenantRegistry.findAll()).thenReturn(List.of(academyA));
    long delayMillis = 150;
    when(operationalMetricsPort.activeDatabaseConnections(List.of(academyA)))
        .thenAnswer(invocation -> sleepThenReturn(delayMillis, 1));
    when(operationalMetricsPort.apiCallMetrics(List.of(academyA), DashboardPeriod.LAST_HOUR))
        .thenAnswer(invocation -> sleepThenReturn(delayMillis, List.<com.academy.mudogroupware.platform.domain.model.ApiCallMetric>of()));
    when(operationalMetricsPort.p95ResponseMilliseconds(List.of(academyA), DashboardPeriod.LAST_HOUR))
        .thenAnswer(invocation -> sleepThenReturn(delayMillis, 10.0));
    when(operationalMetricsPort.errorRatePercent(List.of(academyA), DashboardPeriod.LAST_HOUR))
        .thenAnswer(invocation -> sleepThenReturn(delayMillis, 1.0));
    when(ecsHeadroomPort.findHeadrooms(List.of(academyA)))
        .thenAnswer(invocation -> sleepThenReturn(delayMillis, List.<com.academy.mudogroupware.platform.domain.model.OperationalMetrics.EcsHostHeadroom>of()));
    PlatformDashboardQueryService parallelService = new PlatformDashboardQueryService(
        tenantRegistry, operationalMetricsPort, memberCountMetricsPort, storageUsagePort,
        databaseUsageMetricsPort, ecsHeadroomPort, apiCallFrequencyPort, Executors.newFixedThreadPool(5));

    long start = System.currentTimeMillis();
    parallelService.operationalMetrics(DashboardScope.ALL, null, DashboardPeriod.LAST_HOUR);
    long elapsedMillis = System.currentTimeMillis() - start;

    assertThat(elapsedMillis).isLessThan(delayMillis * 3);
  }

  private static <T> T sleepThenReturn(long millis, T value) throws InterruptedException {
    Thread.sleep(millis);
    return value;
  }

  @Test
  void operationalMetricsDeduplicatesSharedRdsConnectionBudget() {
    AcademyRuntime academyA = academy("academy-a");
    AcademyRuntime academyB = academy("academy-b");
    when(tenantRegistry.findAll()).thenReturn(List.of(academyA, academyB));
    when(operationalMetricsPort.activeDatabaseConnections(List.of(academyA, academyB))).thenReturn(10);
    when(operationalMetricsPort.apiCallMetrics(List.of(academyA, academyB), DashboardPeriod.LAST_HOUR))
        .thenReturn(List.of());
    when(ecsHeadroomPort.findHeadrooms(List.of(academyA, academyB))).thenReturn(List.of());

    var result = service.operationalMetrics(DashboardScope.ALL, null, DashboardPeriod.LAST_HOUR);

    assertThat(result.rdsConnectionBudget().safeBudget()).isEqualTo(100);
    assertThat(result.rdsConnectionBudget().usedPercent()).isEqualTo(10.0);
  }

  @Test
  void activeMemberCountQueriesOnlySelectedAcademy() {
    AcademyRuntime academy = academy("academy-a");
    when(tenantRegistry.get("academy-a")).thenReturn(academy);
    when(memberCountMetricsPort.activeMemberCount(List.of("academy-a"))).thenReturn(12L);

    long result = service.activeMemberCount("academy-a");

    assertThat(result).isEqualTo(12L);
    verify(memberCountMetricsPort).activeMemberCount(List.of("academy-a"));
  }

  @Test
  void operationalMetricsRequiresAcademyCodeWhenScopeIsAcademy() {
    assertThatThrownBy(() -> service.operationalMetrics(DashboardScope.ACADEMY, null, DashboardPeriod.LAST_HOUR))
        .isInstanceOf(PlatformException.class)
        .extracting(exception -> ((PlatformException) exception).getErrorCode())
        .isEqualTo(PlatformErrorCode.ACADEMY_CODE_REQUIRED);
  }

  @Test
  void academiesReturnsAllFromTenantRegistry() {
    AcademyRuntime academyA = academy("academy-a");
    AcademyRuntime academyB = academy("academy-b");
    when(tenantRegistry.findAll()).thenReturn(List.of(academyA, academyB));

    var result = service.academies();

    assertThat(result).containsExactly(academyA, academyB);
  }

  @Test
  void storageUsageCombinesDatabaseAndS3BytesForSelectedAcademy() {
    AcademyRuntime academy = academy("academy-a");
    when(tenantRegistry.get("academy-a")).thenReturn(academy);
    when(databaseUsageMetricsPort.databaseBytes(List.of("academy-a"))).thenReturn(1000L);
    when(storageUsagePort.s3Bytes(academy)).thenReturn(2000L);

    StorageUsage result = service.storageUsage("academy-a");

    assertThat(result.academyCode()).isEqualTo("academy-a");
    assertThat(result.databaseBytes()).isEqualTo(1000L);
    assertThat(result.s3Bytes()).isEqualTo(2000L);
  }

  private AcademyRuntime academy(String code) {
    return new AcademyRuntime(
        code,
        "mudo-prod-cluster",
        "mudo-prod-svc-" + code,
        "mudo-prod-rds-cell-1",
        144,
        0.7,
        "mudo-prod-staff",
        "mudo-prod-finance",
        "tenants/" + code + "/");
  }
}
