package com.academy.mudogroupware.resourceusage.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageEvent;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({ResourceUsagePersistenceAdapter.class, TimeConfig.class})
class ResourceUsagePersistenceAdapterDataJpaTest {

    @Autowired
    private ResourceUsagePersistenceAdapter adapter;

    @Test
    void savesEventsAndSummarizesOnlyRequestedMonth() {
        adapter.save(ResourceUsageEvent.aiTokens("approval-attachment-summary", "GEMINI", "gemini-test",
                100, 20, 120, LocalDateTime.of(2026, 8, 5, 10, 0)));
        adapter.save(ResourceUsageEvent.aiTokens("approval-attachment-summary", "GEMINI", "gemini-test",
                200, 30, 230, LocalDateTime.of(2026, 8, 6, 10, 0)));
        adapter.save(ResourceUsageEvent.smsMessages("rollcall-attendance-sms", 3,
                LocalDateTime.of(2026, 8, 7, 10, 0)));
        adapter.save(ResourceUsageEvent.aiTokens("approval-attachment-summary", "GEMINI", "gemini-test",
                999, 999, 1998, LocalDateTime.of(2026, 9, 1, 0, 0)));

        List<ResourceUsageFeatureSummary> summaries = adapter.summarizeByFeature(
                LocalDateTime.of(2026, 8, 1, 0, 0),
                LocalDateTime.of(2026, 9, 1, 0, 0));

        assertThat(summaries).hasSize(2);
        assertThat(summaries).filteredOn(summary -> summary.resourceType() == ResourceUsageType.AI_TOKEN)
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.feature()).isEqualTo("approval-attachment-summary");
                    assertThat(summary.eventCount()).isEqualTo(2);
                    assertThat(summary.totalAmount()).isEqualTo(350);
                    assertThat(summary.promptTokens()).isEqualTo(300);
                    assertThat(summary.outputTokens()).isEqualTo(50);
                    assertThat(summary.totalTokens()).isEqualTo(350);
                });
        assertThat(summaries).filteredOn(summary -> summary.resourceType() == ResourceUsageType.SMS)
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.feature()).isEqualTo("rollcall-attendance-sms");
                    assertThat(summary.eventCount()).isEqualTo(1);
                    assertThat(summary.totalAmount()).isEqualTo(3);
                });
    }
}
