package com.academy.mudogroupware.platform.application.service;

import com.academy.mudogroupware.platform.application.port.EcsHeadroomPort;
import com.academy.mudogroupware.platform.application.port.DatabaseUsageMetricsPort;
import com.academy.mudogroupware.platform.application.port.MemberCountMetricsPort;
import com.academy.mudogroupware.platform.application.port.OperationalMetricsPort;
import com.academy.mudogroupware.platform.application.port.StorageUsagePort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.domain.model.DashboardScope;
import com.academy.mudogroupware.platform.domain.model.OperationalMetrics;
import com.academy.mudogroupware.platform.domain.model.StorageUsage;
import com.academy.mudogroupware.platform.infrastructure.PlatformTenantRegistry;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PlatformDashboardQueryService {
  private final PlatformTenantRegistry tenantRegistry;
  private final OperationalMetricsPort operationalMetricsPort;
  private final MemberCountMetricsPort memberCountMetricsPort;
  private final StorageUsagePort storageUsagePort;
  private final DatabaseUsageMetricsPort databaseUsageMetricsPort;
  private final EcsHeadroomPort ecsHeadroomPort;

  public List<AcademyRuntime> academies() {
    return tenantRegistry.findAll();
  }

  public OperationalMetrics operationalMetrics(DashboardScope scope, String academyCode, DashboardPeriod period) {
    List<AcademyRuntime> allAcademies = tenantRegistry.findAll();
    List<AcademyRuntime> academies = select(scope, academyCode);
    int activeConnections = operationalMetricsPort.activeDatabaseConnections(academies);
    int safeBudget = academies.stream()
        .collect(Collectors.groupingBy(AcademyRuntime::rdsIdentifier))
        .values().stream()
        .mapToInt(cellAcademies -> safeBudget(cellAcademies.get(0)))
        .sum();
    return new OperationalMetrics(
        scope,
        scope == DashboardScope.ACADEMY ? academyCode : null,
        period,
        operationalMetricsPort.apiCallMetrics(allAcademies, period),
        operationalMetricsPort.p95ResponseMilliseconds(academies, period),
        operationalMetricsPort.errorRatePercent(academies, period),
        new OperationalMetrics.RdsConnectionBudget(activeConnections, safeBudget,
            safeBudget == 0 ? 0 : activeConnections * 100.0 / safeBudget),
        ecsHeadroomPort.findHeadrooms(allAcademies).stream()
            .filter(headroom -> scope == DashboardScope.ALL
                || headroom.academyCodes().contains(academyCode))
            .toList());
  }

  public long activeMemberCount(String academyCode) {
    tenantRegistry.get(academyCode);
    return memberCountMetricsPort.activeMemberCount(List.of(academyCode));
  }

  public StorageUsage storageUsage(String academyCode) {
    AcademyRuntime academy = tenantRegistry.get(academyCode);
    return new StorageUsage(academy.code(), databaseUsageMetricsPort.databaseBytes(List.of(academy.code())),
        storageUsagePort.s3Bytes(academy), Instant.now());
  }

  private List<AcademyRuntime> select(DashboardScope scope, String academyCode) {
    if (scope == DashboardScope.ALL) return tenantRegistry.findAll();
    if (academyCode == null || academyCode.isBlank()) {
      throw new PlatformException(PlatformErrorCode.ACADEMY_CODE_REQUIRED);
    }
    return List.of(tenantRegistry.get(academyCode));
  }

  private int safeBudget(AcademyRuntime academy) {
    return (int) Math.floor(academy.rdsMaxConnections() * academy.rdsAppConnectionRatio());
  }
}
