package com.academy.mudogroupware.revenuereport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

class CountUnreadRevenueReportsServiceTest {

    private final RevenueReportRepository revenueReportRepository = mock(RevenueReportRepository.class);
    private final CountUnreadRevenueReportsService service =
            new CountUnreadRevenueReportsService(revenueReportRepository);

    @Test
    void returnsUnreadCount() {
        when(revenueReportRepository.countUnread()).thenReturn(3L);

        assertThat(service.countUnread()).isEqualTo(3L);
    }
}
