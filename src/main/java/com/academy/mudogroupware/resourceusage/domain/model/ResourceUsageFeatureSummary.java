package com.academy.mudogroupware.resourceusage.domain.model;

public record ResourceUsageFeatureSummary(
        ResourceUsageType resourceType,
        String feature,
        long eventCount,
        long totalAmount,
        long promptTokens,
        long outputTokens,
        long totalTokens
) {
}
