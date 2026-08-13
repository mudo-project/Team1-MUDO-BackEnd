package com.academy.mudogroupware.revenuereport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

class ListRevenueReportsServiceTest {

    private final RevenueReportRepository revenueReportRepository = mock(RevenueReportRepository.class);
    private final ListRevenueReportsService service = new ListRevenueReportsService(revenueReportRepository);

    @Test
    void returnsReportsOrderedByTargetMonthDesc() {
        RevenueReport report = RevenueReport.create(LocalDate.of(2026, 8, 1), "리포트", "{}", LocalDateTime.now());
        when(revenueReportRepository.findAllOrderByTargetMonthDesc()).thenReturn(List.of(report));

        List<RevenueReport> result = service.listReports();

        assertThat(result).containsExactly(report);
    }
}
