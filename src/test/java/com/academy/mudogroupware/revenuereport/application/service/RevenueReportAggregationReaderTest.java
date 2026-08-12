package com.academy.mudogroupware.revenuereport.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.application.port.ActiveEnrollmentCountPort;
import com.academy.mudogroupware.revenuereport.application.port.EnrollmentLectureLookupPort;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummaryPort;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenuePort;
import com.academy.mudogroupware.revenuereport.domain.model.Payment;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentMethod;
import com.academy.mudogroupware.revenuereport.domain.model.PaymentStatus;
import com.academy.mudogroupware.revenuereport.domain.repository.PaymentRepository;

class RevenueReportAggregationReaderTest {

    private final LectureRevenuePort lectureRevenuePort = mock(LectureRevenuePort.class);
    private final ActiveEnrollmentCountPort activeEnrollmentCountPort = mock(ActiveEnrollmentCountPort.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final ExpenseSummaryPort expenseSummaryPort = mock(ExpenseSummaryPort.class);
    private final EnrollmentLectureLookupPort enrollmentLectureLookupPort = mock(EnrollmentLectureLookupPort.class);
    private final RevenueReportAggregationReader reader = new RevenueReportAggregationReader(
            lectureRevenuePort, activeEnrollmentCountPort, paymentRepository, expenseSummaryPort,
            enrollmentLectureLookupPort);

    @Test
    void assemblesAggregationFromAllFivePorts() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(lectureRevenuePort.findAll()).thenReturn(
                List.of(new LectureRevenueInfo(1L, "중등 수학 심화반", "김강사", 300000)));
        when(activeEnrollmentCountPort.countActiveByLectureIds(List.of(1L))).thenReturn(Map.of(1L, 10L));
        Payment payment = Payment.restore(1L, 100L, 300000, from.plusDays(4), PaymentMethod.CARD,
                PaymentStatus.PAID, null, null);
        when(paymentRepository.findAllByPaidAtBetween(from, to)).thenReturn(List.of(payment));
        when(expenseSummaryPort.summarize(from, to)).thenReturn(new ExpenseSummary(50000L, List.of()));
        when(enrollmentLectureLookupPort.findLectureIdsByEnrollmentIds(List.of(100L))).thenReturn(Map.of(100L, 1L));

        RevenueReportAggregation result = reader.read(from, to);

        assertThat(result.lectures()).extracting("lectureId").containsExactly(1L);
        assertThat(result.activeEnrollmentCounts()).containsEntry(1L, 10L);
        assertThat(result.payments()).containsExactly(payment);
        assertThat(result.expenseSummary().totalAmount()).isEqualTo(50000L);
        assertThat(result.enrollmentIdToLectureId()).containsEntry(100L, 1L);
    }
}
