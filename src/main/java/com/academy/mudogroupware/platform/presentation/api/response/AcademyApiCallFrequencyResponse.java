package com.academy.mudogroupware.platform.presentation.api.response;

import com.academy.mudogroupware.platform.domain.model.AcademyApiCallMetrics;
import java.util.List;

public record AcademyApiCallFrequencyResponse(String academyCode, List<ApiCallMetricResponse> apiCallMetrics) {
  public static AcademyApiCallFrequencyResponse from(AcademyApiCallMetrics metrics) {
    return new AcademyApiCallFrequencyResponse(
        metrics.academyCode(),
        metrics.apiCallMetrics().stream().map(ApiCallMetricResponse::from).toList());
  }
}
