package com.academy.mudogroupware.platform.application.service;

import com.academy.mudogroupware.platform.application.port.EcsHeadroomPort;
import com.academy.mudogroupware.platform.application.port.DatabaseUsageMetricsPort;
import com.academy.mudogroupware.platform.application.port.MemberCountMetricsPort;
import com.academy.mudogroupware.platform.application.port.OperationalMetricsPort;
import com.academy.mudogroupware.platform.application.port.StorageUsagePort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.domain.model.DashboardScope;
import com.academy.mudogroupware.platform.domain.model.OperationalMetrics;
import com.academy.mudogroupware.platform.domain.model.StorageUsage;
import com.academy.mudogroupware.platform.infrastructure.PlatformTenantRegistry;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
public class PlatformDashboardQueryService {
  private final PlatformTenantRegistry tenantRegistry;
  private final OperationalMetricsPort operationalMetricsPort;
  private final MemberCountMetricsPort memberCountMetricsPort;
  private final StorageUsagePort storageUsagePort;
  private final DatabaseUsageMetricsPort databaseUsageMetricsPort;
  private final EcsHeadroomPort ecsHeadroomPort;
  private final Executor executor;

  public PlatformDashboardQueryService(
      PlatformTenantRegistry tenantRegistry,
      OperationalMetricsPort operationalMetricsPort,
      MemberCountMetricsPort memberCountMetricsPort,
      StorageUsagePort storageUsagePort,
      DatabaseUsageMetricsPort databaseUsageMetricsPort,
      EcsHeadroomPort ecsHeadroomPort,
      @Qualifier("applicationTaskExecutor") Executor executor) {
    this.tenantRegistry = tenantRegistry;
    this.operationalMetricsPort = operationalMetricsPort;
    this.memberCountMetricsPort = memberCountMetricsPort;
    this.storageUsagePort = storageUsagePort;
    this.databaseUsageMetricsPort = databaseUsageMetricsPort;
    this.ecsHeadroomPort = ecsHeadroomPort;
    this.executor = executor;
  }

  public List<AcademyRuntime> academies() {
    return tenantRegistry.findAll();
  }

  public OperationalMetrics operationalMetrics(DashboardScope scope, String academyCode, DashboardPeriod period) {
    List<AcademyRuntime> allAcademies = tenantRegistry.findAll();
    List<AcademyRuntime> academies = select(scope, academyCode);

    CompletableFuture<Integer> activeConnectionsFuture = CompletableFuture.supplyAsync(
        () -> operationalMetricsPort.activeDatabaseConnections(academies), executor);
    CompletableFuture<List<ApiCallMetric>> apiCallMetricsFuture = CompletableFuture.supplyAsync(
        () -> operationalMetricsPort.apiCallMetrics(allAcademies, period), executor);
    CompletableFuture<Double> p95Future = CompletableFuture.supplyAsync(
        () -> operationalMetricsPort.p95ResponseMilliseconds(academies, period), executor);
    CompletableFuture<Double> errorRateFuture = CompletableFuture.supplyAsync(
        () -> operationalMetricsPort.errorRatePercent(academies, period), executor);
    CompletableFuture<List<OperationalMetrics.EcsHostHeadroom>> ecsHeadroomsFuture = CompletableFuture.supplyAsync(
        () -> ecsHeadroomPort.findHeadrooms(allAcademies), executor);

    int safeBudget = academies.stream()
        .collect(Collectors.groupingBy(AcademyRuntime::rdsIdentifier))
        .values().stream()
        .mapToInt(cellAcademies -> safeBudget(cellAcademies.get(0)))
        .sum();
    int activeConnections = activeConnectionsFuture.join();
    List<OperationalMetrics.EcsHostHeadroom> ecsHeadrooms = ecsHeadroomsFuture.join().stream()
        .filter(headroom -> scope == DashboardScope.ALL
            || headroom.academyCodes().contains(academyCode))
        .toList();

    return new OperationalMetrics(
        scope,
        scope == DashboardScope.ACADEMY ? academyCode : null,
        period,
        apiCallMetricsFuture.join(),
        p95Future.join(),
        errorRateFuture.join(),
        new OperationalMetrics.RdsConnectionBudget(activeConnections, safeBudget,
            safeBudget == 0 ? 0 : activeConnections * 100.0 / safeBudget),
        ecsHeadrooms);
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
