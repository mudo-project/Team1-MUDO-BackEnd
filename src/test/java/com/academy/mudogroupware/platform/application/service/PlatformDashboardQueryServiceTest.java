package com.academy.mudogroupware.platform.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.platform.application.port.DatabaseUsageMetricsPort;
import com.academy.mudogroupware.platform.application.port.EcsHeadroomPort;
import com.academy.mudogroupware.platform.application.port.MemberCountMetricsPort;
import com.academy.mudogroupware.platform.application.port.OperationalMetricsPort;
import com.academy.mudogroupware.platform.application.port.StorageUsagePort;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.domain.model.DashboardScope;
import com.academy.mudogroupware.platform.infrastructure.PlatformTenantRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlatformDashboardQueryServiceTest {
  private final PlatformTenantRegistry tenantRegistry = mock(PlatformTenantRegistry.class);
  private final OperationalMetricsPort operationalMetricsPort = mock(OperationalMetricsPort.class);
  private final MemberCountMetricsPort memberCountMetricsPort = mock(MemberCountMetricsPort.class);
  private final StorageUsagePort storageUsagePort = mock(StorageUsagePort.class);
  private final DatabaseUsageMetricsPort databaseUsageMetricsPort = mock(DatabaseUsageMetricsPort.class);
  private final EcsHeadroomPort ecsHeadroomPort = mock(EcsHeadroomPort.class);

  private PlatformDashboardQueryService service;

  @BeforeEach
  void setUp() {
    service = new PlatformDashboardQueryService(
        tenantRegistry,
        operationalMetricsPort,
        memberCountMetricsPort,
        storageUsagePort,
        databaseUsageMetricsPort,
        ecsHeadroomPort);
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
