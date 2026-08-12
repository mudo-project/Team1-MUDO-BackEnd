package com.academy.mudogroupware.revenuereport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportNotFoundException;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

class GetRevenueReportServiceTest {

    private final RevenueReportRepository revenueReportRepository = mock(RevenueReportRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-02T09:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final GetRevenueReportService service = new GetRevenueReportService(revenueReportRepository, clock);

    @Test
    void marksReportAsReadWhenFetchingDetail() {
        RevenueReport report = RevenueReport.create(LocalDate.of(2026, 8, 1), "리포트", "{}", LocalDateTime.now());
        when(revenueReportRepository.findById(1L)).thenReturn(Optional.of(report));

        service.getReport(1L);

        verify(revenueReportRepository).markRead(1L, LocalDateTime.now(clock));
    }

    @Test
    void throwsWhenReportNotFound() {
        when(revenueReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReport(99L)).isInstanceOf(RevenueReportNotFoundException.class);
    }
}
