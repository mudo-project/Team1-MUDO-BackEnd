package com.academy.mudogroupware.platform.presentation.api.response;

import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.domain.model.DashboardScope;
import com.academy.mudogroupware.platform.domain.model.OperationalMetrics;
import java.util.List;

public record OperationalMetricsResponse(
    DashboardScope scope,
    String academyCode,
    DashboardPeriod period,
    List<ApiCallMetricResponse> apiCallMetrics,
    double p95ResponseMilliseconds,
    double errorRatePercent,
    RdsConnectionBudgetResponse rdsConnectionBudget,
    List<EcsHostHeadroomResponse> ecsHostHeadrooms) {

  public static OperationalMetricsResponse from(OperationalMetrics metrics) {
    return new OperationalMetricsResponse(
        metrics.scope(),
        metrics.academyCode(),
        metrics.period(),
        metrics.apiCallMetrics().stream().map(ApiCallMetricResponse::from).toList(),
        metrics.p95ResponseMilliseconds(),
        metrics.errorRatePercent(),
        RdsConnectionBudgetResponse.from(metrics.rdsConnectionBudget()),
        metrics.ecsHostHeadrooms().stream().map(EcsHostHeadroomResponse::from).toList());
  }

  public record RdsConnectionBudgetResponse(int current, int safeBudget, double usedPercent) {
    private static RdsConnectionBudgetResponse from(OperationalMetrics.RdsConnectionBudget budget) {
      return new RdsConnectionBudgetResponse(budget.current(), budget.safeBudget(), budget.usedPercent());
    }
  }

  public record EcsHostHeadroomResponse(
      String cluster,
      String hostId,
      int registeredCpu,
      int registeredMemoryMib,
      int remainingCpu,
      int remainingMemoryMib,
      List<String> academyCodes) {
    private static EcsHostHeadroomResponse from(OperationalMetrics.EcsHostHeadroom headroom) {
      return new EcsHostHeadroomResponse(
          headroom.cluster(),
          headroom.hostId(),
          headroom.registeredCpu(),
          headroom.registeredMemoryMib(),
          headroom.remainingCpu(),
          headroom.remainingMemoryMib(),
          headroom.academyCodes());
    }
  }
}
