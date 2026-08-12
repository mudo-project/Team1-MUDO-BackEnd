package com.academy.mudogroupware.revenuereport.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.revenuereport.application.port.RevenueReportAiPort;
import com.academy.mudogroupware.revenuereport.application.port.RevenueSnapshot;
import com.academy.mudogroupware.revenuereport.application.usecase.GenerateRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 계산(집계)은 이 서비스와 RevenueSnapshotCalculator가 전담하고, AI는 서술만 한다.
 * AI 호출을 기다리는 동안 DB 커넥션을 잡고 있지 않도록 의도적으로 조회/AI호출/저장을
 * 하나의 트랜잭션으로 묶지 않는다(저장만 트랜잭션).
 */
@Slf4j
@Service
public class GenerateRevenueReportService implements GenerateRevenueReportUseCase {

    private final RevenueReportAggregationReader aggregationReader;
    private final RevenueReportAiPort revenueReportAiPort;
    private final RevenueReportRepository revenueReportRepository;
    private final RevenueSnapshotCalculator calculator;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public GenerateRevenueReportService(RevenueReportAggregationReader aggregationReader,
                                        RevenueReportAiPort revenueReportAiPort,
                                        RevenueReportRepository revenueReportRepository,
                                        RevenueSnapshotCalculator calculator,
                                        Clock clock,
                                        ObjectMapper objectMapper) {
        this.aggregationReader = aggregationReader;
        this.revenueReportAiPort = revenueReportAiPort;
        this.revenueReportRepository = revenueReportRepository;
        this.calculator = calculator;
        this.clock = clock;
        // Spring이 구성한 ObjectMapper(날짜를 [2026,7,1] 배열이 아니라 "2026-07-01" ISO
        // 문자열로 직렬화)를 그대로 복사해 쓴다. 직접 new ObjectMapper()를 만들면 이 설정이
        // 빠져서 실제로 날짜 직렬화 버그를 겪었다. FAIL_ON_UNKNOWN_PROPERTIES도 꺼서, 나중에
        // RevenueSnapshot에 필드를 추가/제거해도 예전 달의 data_snapshot을 읽다가 전월비교가
        // 원인 없이 조용히 사라지는 일이 없게 한다.
        this.objectMapper = objectMapper.copy()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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

            RevenueReportAggregation aggregation = aggregationReader.read(from, to);
            Optional<RevenueSnapshot> previousSnapshot = fetchPreviousSnapshot(targetMonth);

            RevenueSnapshot snapshot = calculator.calculate(targetMonth, aggregation.lectures(),
                    aggregation.activeEnrollmentCounts(), aggregation.payments(),
                    aggregation.enrollmentIdToLectureId(), aggregation.expenseSummary(), previousSnapshot);

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
