package com.academy.mudogroupware.platform.presentation.api.response;

import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;

public record ApiCallMetricResponse(String category, long count) {
  public static ApiCallMetricResponse from(ApiCallMetric metric) {
    return new ApiCallMetricResponse(metric.category(), metric.count());
  }
}
