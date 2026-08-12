package com.academy.mudogroupware.resourceusage.application.query;

import java.util.List;

import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

public record ResourceUsageResourceSummaryView(
        ResourceUsageType resourceType,
        String unit,
        long totalAmount,
        List<ResourceUsageFeatureSummary> features
) {
}
