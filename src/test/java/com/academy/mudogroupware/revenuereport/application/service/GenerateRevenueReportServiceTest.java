package com.academy.mudogroupware.revenuereport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.application.port.ActiveEnrollmentCountPort;
import com.academy.mudogroupware.revenuereport.application.port.EnrollmentLectureLookupPort;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummaryPort;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenuePort;
import com.academy.mudogroupware.revenuereport.application.port.RevenueReportAiPort;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.PaymentRepository;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

class GenerateRevenueReportServiceTest {

    private final LectureRevenuePort lectureRevenuePort = mock(LectureRevenuePort.class);
    private final ActiveEnrollmentCountPort activeEnrollmentCountPort = mock(ActiveEnrollmentCountPort.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final ExpenseSummaryPort expenseSummaryPort = mock(ExpenseSummaryPort.class);
    private final RevenueReportAiPort revenueReportAiPort = mock(RevenueReportAiPort.class);
    private final EnrollmentLectureLookupPort enrollmentLectureLookupPort = mock(EnrollmentLectureLookupPort.class);
    private final RevenueReportRepository revenueReportRepository = mock(RevenueReportRepository.class);
    private final RevenueSnapshotCalculator calculator = new RevenueSnapshotCalculator();
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-01T00:30:00Z"), ZoneId.of("Asia/Seoul"));
    private final GenerateRevenueReportService service = new GenerateRevenueReportService(
            lectureRevenuePort, activeEnrollmentCountPort, paymentRepository, expenseSummaryPort,
            revenueReportAiPort, enrollmentLectureLookupPort, revenueReportRepository, calculator, clock);

    @Test
    void skipsWhenReportAlreadyExistsForTargetMonth() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth))
                .thenReturn(Optional.of(RevenueReport.create(targetMonth, "이미 있음", "{}", LocalDateTime.now())));

        service.generate(targetMonth);

        verify(revenueReportAiPort, never()).generateReport(any());
        verify(revenueReportRepository, never()).save(any());
    }

    @Test
    void aggregatesCallsAiAndSavesWhenNoExistingReport() {
        LocalDate targetMonth = LocalDate.of(2026, 8, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth)).thenReturn(Optional.empty());
        when(revenueReportRepository.findByTargetMonth(LocalDate.of(2026, 7, 1))).thenReturn(Optional.empty());
        when(lectureRevenuePort.findAll()).thenReturn(
                List.of(new LectureRevenueInfo(1L, "중등 수학 심화반", "김강사", 300000)));
        when(activeEnrollmentCountPort.countActiveByLectureIds(List.of(1L))).thenReturn(Map.of(1L, 10L));
        when(paymentRepository.findAllByPaidAtBetween(any(), any())).thenReturn(List.of());
        when(expenseSummaryPort.summarize(any(), any())).thenReturn(new ExpenseSummary(0L, List.of()));
        when(enrollmentLectureLookupPort.findLectureIdsByEnrollmentIds(any())).thenReturn(Map.of());
        when(revenueReportAiPort.generateReport(any())).thenReturn("8월 매출 리포트 텍스트");
        when(revenueReportRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generate(targetMonth);

        verify(revenueReportRepository).save(any());
        verify(revenueReportAiPort).generateReport(any());
    }
}
