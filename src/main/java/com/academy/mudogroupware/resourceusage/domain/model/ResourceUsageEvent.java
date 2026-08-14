package com.academy.mudogroupware.resourceusage.domain.model;

import java.time.LocalDateTime;

public class ResourceUsageEvent {

    private final Long id;
    private final ResourceUsageType resourceType;
    private final String feature;
    private final long amount;
    private final String provider;
    private final String modelName;
    private final long promptTokens;
    private final long outputTokens;
    private final long totalTokens;
    private final LocalDateTime occurredAt;

    private ResourceUsageEvent(Long id, ResourceUsageType resourceType, String feature, long amount,
                               String provider, String modelName, long promptTokens, long outputTokens,
                               long totalTokens, LocalDateTime occurredAt) {
        if (resourceType == null) {
            throw new IllegalArgumentException("resourceType must not be null");
        }
        if (feature == null || feature.isBlank()) {
            throw new IllegalArgumentException("feature must not be blank");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }
        this.id = id;
        this.resourceType = resourceType;
        this.feature = feature;
        this.amount = amount;
        this.provider = provider;
        this.modelName = modelName;
        this.promptTokens = promptTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.occurredAt = occurredAt;
    }

    public static ResourceUsageEvent aiTokens(String feature, String provider, String modelName,
                                              long promptTokens, long outputTokens, long totalTokens,
                                              LocalDateTime occurredAt) {
        return new ResourceUsageEvent(null, ResourceUsageType.AI_TOKEN, feature, totalTokens,
                provider, modelName, promptTokens, outputTokens, totalTokens, occurredAt);
    }

    public static ResourceUsageEvent smsMessages(String feature, long sentCount, LocalDateTime occurredAt) {
        return new ResourceUsageEvent(null, ResourceUsageType.SMS, feature, sentCount,
                null, null, 0, 0, 0, occurredAt);
    }

    public static ResourceUsageEvent s3Storage(String feature, long bytes, LocalDateTime occurredAt) {
        return new ResourceUsageEvent(null, ResourceUsageType.S3_STORAGE, feature, bytes,
                null, null, 0, 0, 0, occurredAt);
    }

    public static ResourceUsageEvent mail(String feature, long count, LocalDateTime occurredAt) {
        return new ResourceUsageEvent(null, ResourceUsageType.MAIL, feature, count,
                null, null, 0, 0, 0, occurredAt);
    }

    public static ResourceUsageEvent restore(Long id, ResourceUsageType resourceType, String feature, long amount,
                                             String provider, String modelName, long promptTokens, long outputTokens,
                                             long totalTokens, LocalDateTime occurredAt) {
        return new ResourceUsageEvent(id, resourceType, feature, amount, provider, modelName,
                promptTokens, outputTokens, totalTokens, occurredAt);
    }

    public Long getId() {
        return id;
    }

    public ResourceUsageType getResourceType() {
        return resourceType;
    }

    public String getFeature() {
        return feature;
    }

    public long getAmount() {
        return amount;
    }

    public String getProvider() {
        return provider;
    }

    public String getModelName() {
        return modelName;
    }

    public long getPromptTokens() {
        return promptTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
