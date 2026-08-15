package com.academy.mudogroupware.resourceusage.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

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

    @Query("""
            select coalesce(sum(e.amount), 0)
            from ResourceUsageEventEntity e
            where e.resourceType = :type
            """)
    long sumByType(@Param("type") ResourceUsageType type);

    @Query("""
            select coalesce(sum(e.amount), 0)
            from ResourceUsageEventEntity e
            where e.resourceType = :type
              and e.occurredAt >= :fromInclusive
              and e.occurredAt < :toExclusive
            """)
    long sumByTypeAndPeriod(@Param("type") ResourceUsageType type,
                             @Param("fromInclusive") LocalDateTime fromInclusive,
                             @Param("toExclusive") LocalDateTime toExclusive);
}
