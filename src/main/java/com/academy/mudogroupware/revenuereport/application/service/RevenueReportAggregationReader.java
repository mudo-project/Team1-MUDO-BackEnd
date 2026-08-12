package com.academy.mudogroupware.revenuereport.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.revenuereport.application.port.ActiveEnrollmentCountPort;
import com.academy.mudogroupware.revenuereport.application.port.EnrollmentLectureLookupPort;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummaryPort;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenuePort;
import com.academy.mudogroupware.revenuereport.domain.model.Payment;
import com.academy.mudogroupware.revenuereport.domain.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 강의/활성수강/결제/지출/등록-강의매핑 조회를 하나의 읽기전용 트랜잭션으로 묶는다. 예전에는
 * 각각 별개 트랜잭션(또는 트랜잭션 없음)으로 조회해서, 조회 사이에 데이터가 바뀌면 서로 다른
 * 시점의 값이 한 스냅샷에 섞일 수 있었다. AI 호출/저장은 이 트랜잭션 밖에 그대로 둔다(커넥션을
 * AI 응답 대기 중에 붙잡지 않기 위해 — GenerateRevenueReportService 참고).
 */
@Component
@RequiredArgsConstructor
class RevenueReportAggregationReader {

    private final LectureRevenuePort lectureRevenuePort;
    private final ActiveEnrollmentCountPort activeEnrollmentCountPort;
    private final PaymentRepository paymentRepository;
    private final ExpenseSummaryPort expenseSummaryPort;
    private final EnrollmentLectureLookupPort enrollmentLectureLookupPort;

    @Transactional(readOnly = true)
    RevenueReportAggregation read(LocalDateTime from, LocalDateTime to) {
        List<LectureRevenueInfo> lectures = lectureRevenuePort.findAll();
        List<Long> lectureIds = lectures.stream().map(LectureRevenueInfo::lectureId).toList();
        Map<Long, Long> activeEnrollmentCounts = activeEnrollmentCountPort.countActiveByLectureIds(lectureIds);
        List<Payment> payments = paymentRepository.findAllByPaidAtBetween(from, to);
        ExpenseSummary expenseSummary = expenseSummaryPort.summarize(from, to);

        List<Long> enrollmentIds = payments.stream().map(Payment::getEnrollmentId).distinct().toList();
        Map<Long, Long> enrollmentIdToLectureId =
                enrollmentLectureLookupPort.findLectureIdsByEnrollmentIds(enrollmentIds);

        return new RevenueReportAggregation(lectures, activeEnrollmentCounts, payments, expenseSummary,
                enrollmentIdToLectureId);
    }
}
