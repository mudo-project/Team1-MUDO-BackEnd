package com.academy.mudogroupware.resourceusage.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageEvent;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({ResourceUsagePersistenceAdapter.class, ResourceUsagePersistenceAdapterDataJpaTest.AuditingConfig.class})
class ResourceUsagePersistenceAdapterDataJpaTest {

    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingConfig {
    }

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 15, 10, 0);

    @Autowired
    private ResourceUsagePersistenceAdapter resourceUsageRepository;

    @Test
    void sumByTypeAddsAllEventsRegardlessOfTime() {
        resourceUsageRepository.save(ResourceUsageEvent.s3Storage("a", 100L, NOW.minusMonths(3)));
        resourceUsageRepository.save(ResourceUsageEvent.s3Storage("b", 200L, NOW));
        resourceUsageRepository.save(ResourceUsageEvent.smsMessages("c", 5L, NOW));

        assertThat(resourceUsageRepository.sumByType(ResourceUsageType.S3_STORAGE)).isEqualTo(300L);
    }

    @Test
    void sumByTypeAndPeriodOnlyCountsWithinRange() {
        resourceUsageRepository.save(ResourceUsageEvent.smsMessages("a", 10L, NOW.minusMonths(1)));
        resourceUsageRepository.save(ResourceUsageEvent.smsMessages("b", 20L, NOW));

        long sum = resourceUsageRepository.sumByTypeAndPeriod(
                ResourceUsageType.SMS, NOW.toLocalDate().atStartOfDay(), NOW.plusDays(1));

        assertThat(sum).isEqualTo(20L);
    }

    @Test
    void sumByTypeAndPeriodIncludesFromInclusiveAndExcludesToExclusiveBoundary() {
        LocalDateTime from = NOW.toLocalDate().atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        resourceUsageRepository.save(ResourceUsageEvent.smsMessages("at-from", 10L, from));
        resourceUsageRepository.save(ResourceUsageEvent.smsMessages("at-to", 20L, to));

        long sum = resourceUsageRepository.sumByTypeAndPeriod(ResourceUsageType.SMS, from, to);

        assertThat(sum).isEqualTo(10L);
    }

    @Test
    void sumsAreZeroWhenNoEvents() {
        assertThat(resourceUsageRepository.sumByType(ResourceUsageType.MAIL)).isZero();
    }
}
