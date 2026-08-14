package com.academy.mudogroupware.resourceusage.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public interface ResourceUsageRepository {

    ResourceUsageEvent save(ResourceUsageEvent event);

    List<ResourceUsageFeatureSummary> summarizeByFeature(LocalDateTime fromInclusive, LocalDateTime toExclusive);

    long sumByType(ResourceUsageType type);

    long sumByTypeAndPeriod(ResourceUsageType type, LocalDateTime fromInclusive, LocalDateTime toExclusive);
}
