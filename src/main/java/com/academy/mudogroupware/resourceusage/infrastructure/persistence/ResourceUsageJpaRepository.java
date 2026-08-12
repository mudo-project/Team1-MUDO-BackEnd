package com.academy.mudogroupware.resourceusage.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;

public interface ResourceUsageJpaRepository extends JpaRepository<ResourceUsageEventEntity, Long> {

    @Query("""
            select new com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary(
                e.resourceType,
                e.feature,
                count(e),
                sum(e.amount),
                sum(e.promptTokens),
                sum(e.outputTokens),
                sum(e.totalTokens)
            )
            from ResourceUsageEventEntity e
            where e.occurredAt >= :fromInclusive
              and e.occurredAt < :toExclusive
            group by e.resourceType, e.feature
            order by e.resourceType asc, e.feature asc
            """)
    List<ResourceUsageFeatureSummary> summarizeByFeature(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive);
}
