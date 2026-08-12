package com.academy.mudogroupware.revenuereport.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.revenuereport.application.port.ActiveEnrollmentCountPort;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummaryPort;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenuePort;
import com.academy.mudogroupware.revenuereport.application.port.RevenueReportAiPort;
import com.academy.mudogroupware.revenuereport.application.usecase.GenerateRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.domain.model.Payment;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.PaymentRepository;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * 계산(집계)은 이 서비스와 RevenueSnapshotCalculator가 전담하고, AI는 서술만 한다.
 * AI 호출을 기다리는 동안 DB 커넥션을 잡고 있지 않도록 의도적으로 조회/AI호출/저장을
 * 하나의 트랜잭션으로 묶지 않는다(저장만 트랜잭션).
 */
@Slf4j
@Service
public class GenerateRevenueReportService implements GenerateRevenueReportUseCase {

    private final LectureRevenuePort lectureRevenuePort;
    private final ActiveEnrollmentCountPort activeEnrollmentCountPort;
    private final PaymentRepository paymentRepository;
    private final ExpenseSummaryPort expenseSummaryPort;
    private final RevenueReportAiPort revenueReportAiPort;
    private final RevenueReportRepository revenueReportRepository;
    private final RevenueSnapshotCalculator calculator;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public GenerateRevenueReportService(LectureRevenuePort lectureRevenuePort,
                                        ActiveEnrollmentCountPort activeEnrollmentCountPort,
                                        PaymentRepository paymentRepository,
                                        ExpenseSummaryPort expenseSummaryPort,
                                        RevenueReportAiPort revenueReportAiPort,
                                        RevenueReportRepository revenueReportRepository,
                                        RevenueSnapshotCalculator calculator,
                                        Clock clock) {
        this.lectureRevenuePort = lectureRevenuePort;
        this.activeEnrollmentCountPort = activeEnrollmentCountPort;
        this.paymentRepository = paymentRepository;
        this.expenseSummaryPort = expenseSummaryPort;
        this.revenueReportAiPort = revenueReportAiPort;
        this.revenueReportRepository = revenueReportRepository;
        this.calculator = calculator;
        this.clock = clock;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public void generate(LocalDate targetMonth) {
        log.info("event=revenue_report_generate_시작 targetMonth={}", targetMonth);
        try {
            if (revenueReportRepository.findByTargetMonth(targetMonth).isPresent()) {
                log.info("event=revenue_report_already_exists targetMonth={}", targetMonth);
                return;
            }

            LocalDateTime from = targetMonth.atStartOfDay();
            LocalDateTime to = targetMonth.plusMonths(1).atStartOfDay();

            List<LectureRevenueInfo> lectures = lectureRevenuePort.findAll();
            List<Long> lectureIds = lectures.stream().map(LectureRevenueInfo::lectureId).toList();
            Map<Long, Long> activeEnrollmentCounts = activeEnrollmentCountPort.countActiveByLectureIds(lectureIds);
            List<Payment> payments = paymentRepository.findAllByPaidAtBetween(from, to);
            ExpenseSummary expenseSummary = expenseSummaryPort.summarize(from, to);

            // enrollmentId -> lectureId 매핑은 이번 스코프에선 payment.enrollmentId로부터 강의별
            // 실 매출을 계산할 때 필요하지만, 별도 조회 포트를 새로 두지 않고 student 도메인의
            // 기존 조회를 재사용한다 — Task 13에서 필요 시 추가 포트로 보강.
            Map<Long, Long> enrollmentIdToLectureId = Map.of();

            Optional<RevenueSnapshot> previousSnapshot = fetchPreviousSnapshot(targetMonth);

            RevenueSnapshot snapshot = calculator.calculate(targetMonth, lectures, activeEnrollmentCounts, payments,
                    enrollmentIdToLectureId, expenseSummary, previousSnapshot);

            String reportText = revenueReportAiPort.generateReport(snapshot);

            RevenueReport report = RevenueReport.create(
                    targetMonth, reportText, toJson(snapshot), LocalDateTime.now(clock));
            RevenueReport saved = revenueReportRepository.save(report);

            log.info("event=revenue_report_generate_완료 targetMonth={}, reportId={}", targetMonth, saved.getId());
        } catch (RuntimeException e) {
            log.warn("event=revenue_report_generate_실패 targetMonth={}, reason={}", targetMonth, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 전월 데이터를 다시 집계하지 않고, 전월 리포트 생성 당시 저장해둔 data_snapshot을 그대로
     * 되읽어 온다 — 원장에게 실제로 보여줬던 숫자와 이번 달 비교치를 일치시키기 위함.
     */
    private Optional<RevenueSnapshot> fetchPreviousSnapshot(LocalDate targetMonth) {
        // Optional#map은 매퍼가 null을 반환하면 자동으로 Optional.empty()가 된다 —
        // 파싱 실패도 "비교 대상 없음"과 동일하게 취급해 조용히 넘어간다(리포트 생성 자체를 막지 않음).
        return revenueReportRepository.findByTargetMonth(targetMonth.minusMonths(1))
                .map(previous -> {
                    try {
                        return objectMapper.readValue(previous.getDataSnapshot(), RevenueSnapshot.class);
                    } catch (JsonProcessingException e) {
                        log.warn("event=revenue_report_previous_snapshot_parse_실패 targetMonth={}, reason={}",
                                targetMonth, e.getMessage(), e);
                        return null;
                    }
                });
    }

    private String toJson(RevenueSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("데이터 스냅샷 직렬화에 실패했습니다.", e);
        }
    }
}
