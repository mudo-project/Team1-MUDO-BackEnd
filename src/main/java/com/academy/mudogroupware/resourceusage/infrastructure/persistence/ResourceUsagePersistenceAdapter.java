package com.academy.mudogroupware.resourceusage.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageEvent;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ResourceUsagePersistenceAdapter implements ResourceUsageRepository {

    private final ResourceUsageJpaRepository resourceUsageJpaRepository;

    @Override
    public ResourceUsageEvent save(ResourceUsageEvent event) {
        return toDomain(resourceUsageJpaRepository.save(toEntity(event)));
    }

    @Override
    public List<ResourceUsageFeatureSummary> summarizeByFeature(LocalDateTime fromInclusive,
                                                                LocalDateTime toExclusive) {
        return resourceUsageJpaRepository.summarizeByFeature(fromInclusive, toExclusive);
    }

    private ResourceUsageEventEntity toEntity(ResourceUsageEvent event) {
        return ResourceUsageEventEntity.builder()
                .id(event.getId())
                .resourceType(event.getResourceType())
                .feature(event.getFeature())
                .amount(event.getAmount())
                .unit(event.getResourceType().unit())
                .provider(event.getProvider())
                .modelName(event.getModelName())
                .promptTokens(event.getPromptTokens())
                .outputTokens(event.getOutputTokens())
                .totalTokens(event.getTotalTokens())
                .occurredAt(event.getOccurredAt())
                .build();
    }

    private ResourceUsageEvent toDomain(ResourceUsageEventEntity entity) {
        return ResourceUsageEvent.restore(
                entity.getId(),
                entity.getResourceType(),
                entity.getFeature(),
                entity.getAmount(),
                entity.getProvider(),
                entity.getModelName(),
                entity.getPromptTokens(),
                entity.getOutputTokens(),
                entity.getTotalTokens(),
                entity.getOccurredAt());
    }
}
