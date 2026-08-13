package com.academy.mudogroupware.platform.domain.model;

import java.util.List;

public record AcademyApiCallMetrics(String academyCode, List<ApiCallMetric> apiCallMetrics) {}
