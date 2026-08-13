package com.academy.mudogroupware.resourceusage.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.resourceusage.application.query.MonthlyResourceUsageView;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageFeatureSummary;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageRepository;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;

class ResourceUsageQueryServiceTest {

    private final ResourceUsageRepository resourceUsageRepository = mock(ResourceUsageRepository.class);
    private final ResourceUsageQueryService service = new ResourceUsageQueryService(resourceUsageRepository);

    @Test
    void groupsMonthlyUsageByResourceTypeAndFeature() {
        YearMonth month = YearMonth.of(2026, 8);
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(resourceUsageRepository.summarizeByFeature(from, to)).thenReturn(List.of(
                new ResourceUsageFeatureSummary(ResourceUsageType.AI_TOKEN, "approval-attachment-summary",
                        30, 70350, 33510, 3060, 70350),
                new ResourceUsageFeatureSummary(ResourceUsageType.AI_TOKEN, "dataimport-onboarding-analysis",
                        1, 3128, 1519, 213, 3128),
                new ResourceUsageFeatureSummary(ResourceUsageType.SMS, "rollcall-attendance-sms",
                        12, 150, 0, 0, 0)));

        MonthlyResourceUsageView view = service.getMonthlyUsage(month);

        assertThat(view.month()).isEqualTo(month);
        assertThat(view.resources()).hasSize(2);
        assertThat(view.resources()).filteredOn(resource -> resource.resourceType() == ResourceUsageType.AI_TOKEN)
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.unit()).isEqualTo("tokens");
                    assertThat(resource.totalAmount()).isEqualTo(73478);
                    assertThat(resource.features()).hasSize(2);
                });
        assertThat(view.resources()).filteredOn(resource -> resource.resourceType() == ResourceUsageType.SMS)
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.unit()).isEqualTo("messages");
                    assertThat(resource.totalAmount()).isEqualTo(150);
                });
    }
}
