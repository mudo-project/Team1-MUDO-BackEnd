package com.academy.mudogroupware.platform.application.port;

import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import java.util.List;

public interface OperationalMetricsPort {
  List<ApiCallMetric> apiCallMetrics(List<AcademyRuntime> academies, DashboardPeriod period);

  double p95ResponseMilliseconds(List<AcademyRuntime> academies, DashboardPeriod period);

  double errorRatePercent(List<AcademyRuntime> academies, DashboardPeriod period);

  int activeDatabaseConnections(List<AcademyRuntime> academies);
}
