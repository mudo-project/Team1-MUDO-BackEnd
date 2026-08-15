package com.academy.mudogroupware.platform.application.service;

import com.academy.mudogroupware.platform.application.port.ApiCallFrequencyPort;
import com.academy.mudogroupware.platform.application.port.EcsHeadroomPort;
import com.academy.mudogroupware.platform.application.port.DatabaseUsageMetricsPort;
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
import com.academy.mudogroupware.platform.domain.model.OperationalMetrics;
import com.academy.mudogroupware.platform.domain.model.StorageUsage;
import com.academy.mudogroupware.platform.infrastructure.PlatformTenantRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
public class PlatformDashboardQueryService {
  private final PlatformTenantRegistry tenantRegistry;
  private final OperationalMetricsPort operationalMetricsPort;
  private final MemberCountMetricsPort memberCountMetricsPort;
  private final StorageUsagePort storageUsagePort;
  private final DatabaseUsageMetricsPort databaseUsageMetricsPort;
  private final EcsHeadroomPort ecsHeadroomPort;
  private final ApiCallFrequencyPort apiCallFrequencyPort;
  private final Executor executor;

  public PlatformDashboardQueryService(
      PlatformTenantRegistry tenantRegistry,
      OperationalMetricsPort operationalMetricsPort,
      MemberCountMetricsPort memberCountMetricsPort,
      StorageUsagePort storageUsagePort,
      DatabaseUsageMetricsPort databaseUsageMetricsPort,
      EcsHeadroomPort ecsHeadroomPort,
      ApiCallFrequencyPort apiCallFrequencyPort,
      @Qualifier("applicationTaskExecutor") Executor executor) {
    this.tenantRegistry = tenantRegistry;
    this.operationalMetricsPort = operationalMetricsPort;
    this.memberCountMetricsPort = memberCountMetricsPort;
    this.storageUsagePort = storageUsagePort;
    this.databaseUsageMetricsPort = databaseUsageMetricsPort;
    this.ecsHeadroomPort = ecsHeadroomPort;
    this.apiCallFrequencyPort = apiCallFrequencyPort;
    this.executor = executor;
  }

  public List<AcademyRuntime> academies() {
    log.info("event=platform_academies_list_시작");
    List<AcademyRuntime> academies = tenantRegistry.findAll();
    log.info("event=platform_academies_list_완료 count={}", academies.size());
    return academies;
  }

  public List<AcademyApiCallMetrics> apiCallFrequency(DashboardScope scope, String academyCode, DashboardPeriod period) {
    log.info("event=platform_api_call_frequency_시작 scope={}, academyCode={}, period={}", scope, academyCode, period);
    List<AcademyRuntime> academies = select(scope, academyCode);
    Map<String, List<ApiCallMetric>> byAcademy = apiCallFrequencyPort.apiCallMetricsByAcademy(academies, period);
    List<AcademyApiCallMetrics> result = academies.stream()
        .map(academy -> new AcademyApiCallMetrics(academy.code(), byAcademy.getOrDefault(academy.code(), List.of())))
        .toList();
    log.info("event=platform_api_call_frequency_완료 scope={}, academyCode={}, period={}, academyCount={}",
        scope, academyCode, period, result.size());
    return result;
  }

  public OperationalMetrics operationalMetrics(DashboardScope scope, String academyCode, DashboardPeriod period) {
    log.info("event=platform_operational_metrics_시작 scope={}, academyCode={}, period={}", scope, academyCode, period);
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

    OperationalMetrics metrics = new OperationalMetrics(
        scope,
        scope == DashboardScope.ACADEMY ? academyCode : null,
        period,
        apiCallMetricsFuture.join(),
        p95Future.join(),
        errorRateFuture.join(),
        new OperationalMetrics.RdsConnectionBudget(activeConnections, safeBudget,
            safeBudget == 0 ? 0 : activeConnections * 100.0 / safeBudget),
        ecsHeadrooms);
    log.info("event=platform_operational_metrics_완료 scope={}, academyCode={}, period={}", scope, academyCode, period);
    return metrics;
  }

  public long activeMemberCount(String academyCode) {
    log.info("event=platform_member_count_시작 academyCode={}", academyCode);
    tenantRegistry.get(academyCode);
    long count = memberCountMetricsPort.activeMemberCount(List.of(academyCode));
    log.info("event=platform_member_count_완료 academyCode={}, count={}", academyCode, count);
    return count;
  }

  public StorageUsage storageUsage(String academyCode) {
    log.info("event=platform_storage_usage_시작 academyCode={}", academyCode);
    AcademyRuntime academy = tenantRegistry.get(academyCode);
    StorageUsage usage = new StorageUsage(academy.code(), databaseUsageMetricsPort.databaseBytes(List.of(academy.code())),
        storageUsagePort.s3Bytes(academy), Instant.now());
    log.info("event=platform_storage_usage_완료 academyCode={}", academyCode);
    return usage;
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
