package com.academy.mudogroupware.platform.application.port;

import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import java.util.List;
import java.util.Map;

public interface ApiCallFrequencyPort {
  Map<String, List<ApiCallMetric>> apiCallMetricsByAcademy(List<AcademyRuntime> academies, DashboardPeriod period);
}
