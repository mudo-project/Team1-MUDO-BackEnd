package com.academy.mudogroupware.resourceusage.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "resource_usage_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResourceUsageEventEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_usage_event_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceUsageType resourceType;

    @Column(name = "feature", nullable = false, length = 100)
    private String feature;

    @Column(name = "amount", nullable = false)
    private long amount;

    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_tokens", nullable = false)
    private long promptTokens;

    @Column(name = "output_tokens", nullable = false)
    private long outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private long totalTokens;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Builder
    private ResourceUsageEventEntity(Long id, ResourceUsageType resourceType, String feature, long amount,
                                     String unit, String provider, String modelName, long promptTokens,
                                     long outputTokens, long totalTokens, LocalDateTime occurredAt) {
        this.id = id;
        this.resourceType = resourceType;
        this.feature = feature;
        this.amount = amount;
        this.unit = unit;
        this.provider = provider;
        this.modelName = modelName;
        this.promptTokens = promptTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.occurredAt = occurredAt;
    }
}
