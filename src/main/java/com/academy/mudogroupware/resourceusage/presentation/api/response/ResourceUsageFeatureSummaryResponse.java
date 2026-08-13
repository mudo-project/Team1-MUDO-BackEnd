package com.academy.mudogroupware.resourceusage.presentation.api.response;

import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;

public record ResourceUsageFeatureSummaryResponse(
        String feature,
        long eventCount,
        long totalAmount,
        long promptTokens,
        long outputTokens,
        long totalTokens
) {

    public static ResourceUsageFeatureSummaryResponse from(ResourceUsageFeatureSummary summary) {
        return new ResourceUsageFeatureSummaryResponse(
                summary.feature(),
                summary.eventCount(),
                summary.totalAmount(),
                summary.promptTokens(),
                summary.outputTokens(),
                summary.totalTokens());
    }
}
