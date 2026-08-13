package com.academy.mudogroupware.platform.domain.model;

import java.util.List;

public record OperationalMetrics(
    DashboardScope scope,
    String academyCode,
    DashboardPeriod period,
    List<ApiCallMetric> apiCallMetrics,
    double p95ResponseMilliseconds,
    double errorRatePercent,
    RdsConnectionBudget rdsConnectionBudget,
    List<EcsHostHeadroom> ecsHostHeadrooms) {
  public record RdsConnectionBudget(int current, int safeBudget, double usedPercent) {}

  public record EcsHostHeadroom(
      String cluster,
      String hostId,
      int registeredCpu,
      int registeredMemoryMib,
      int remainingCpu,
      int remainingMemoryMib,
      List<String> academyCodes) {}
}
