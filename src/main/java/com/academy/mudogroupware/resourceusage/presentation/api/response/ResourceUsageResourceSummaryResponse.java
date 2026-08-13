package com.academy.mudogroupware.resourceusage.presentation.api.response;

import java.util.List;

import com.academy.mudogroupware.resourceusage.application.query.ResourceUsageResourceSummaryView;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

public record ResourceUsageResourceSummaryResponse(
        ResourceUsageType resourceType,
        String unit,
        long totalAmount,
        List<ResourceUsageFeatureSummaryResponse> features
) {

    public static ResourceUsageResourceSummaryResponse from(ResourceUsageResourceSummaryView view) {
        return new ResourceUsageResourceSummaryResponse(
                view.resourceType(),
                view.unit(),
                view.totalAmount(),
                view.features().stream()
                        .map(ResourceUsageFeatureSummaryResponse::from)
                        .toList());
    }
}
