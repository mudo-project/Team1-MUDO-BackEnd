package com.academy.mudogroupware.revenuereport.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.academy.mudogroupware.revenuereport.application.port.AcademyOwnerLookupPort;
import com.academy.mudogroupware.revenuereport.application.port.RevenueReportAiPort;
import com.academy.mudogroupware.revenuereport.application.port.RevenueSnapshot;
import com.academy.mudogroupware.revenuereport.application.usecase.GenerateRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.domain.event.RevenueReportGeneratedEvent;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final MeterRegistry meterRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final AcademyOwnerLookupPort academyOwnerLookupPort;

    public GenerateRevenueReportService(RevenueReportAggregationReader aggregationReader,
                                        RevenueReportAiPort revenueReportAiPort,
                                        RevenueReportRepository revenueReportRepository,
                                        RevenueSnapshotCalculator calculator,
                                        Clock clock,
                                        ObjectMapper objectMapper,
                                        MeterRegistry meterRegistry,
                                        ApplicationEventPublisher eventPublisher,
                                        AcademyOwnerLookupPort academyOwnerLookupPort) {
        this.aggregationReader = aggregationReader;
        this.revenueReportAiPort = revenueReportAiPort;
        this.revenueReportRepository = revenueReportRepository;
        this.calculator = calculator;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
        this.academyOwnerLookupPort = academyOwnerLookupPort;
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
        Timer.Sample sample = Timer.start(meterRegistry);
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
            RevenueReport saved;
            try {
                saved = revenueReportRepository.save(report);
            } catch (DataIntegrityViolationException e) {
                // findByTargetMonth 확인과 save 사이의 경쟁 조건 — 다른 인스턴스가 그 사이 먼저
                // 저장을 마쳤다. target_month unique 제약 위반은 여기서만 발생할 수 있으므로
                // 놀란 실패 로그 대신 "이미 처리됨"과 동일하게 조용히 넘어간다(AI를 두 번 부른
                // 비용은 감수하되, 데이터 정합성과 배치 관측성은 지킨다).
                log.info("event=revenue_report_generate_중복_스킵 targetMonth={}", targetMonth);
                return;
            }

            log.info("event=revenue_report_generate_완료 targetMonth={}, reportId={}", targetMonth, saved.getId());
            recordMetric(sample, "success");
            publishGeneratedEvent(saved);
        } catch (RuntimeException e) {
            log.warn("event=revenue_report_generate_실패 targetMonth={}, reason={}", targetMonth, e.getMessage(), e);
            recordMetric(sample, "failure");
            throw e;
        }
    }

    /**
     * 월 1회만 실행되는 배치라 실패를 놓치면 다음 달까지 리포트가 없다 — 성공/실패 카운터와
     * 소요시간을 남겨서 알림에 연결할 수 있게 한다. 메트릭 기록 자체가 실패해도 배치 결과에는
     * 영향을 주면 안 되므로 예외를 흡수한다.
     */
    private void recordMetric(Timer.Sample sample, String result) {
        try {
            meterRegistry.counter("mudo.revenue.report.generate", "result", result).increment();
            sample.stop(meterRegistry.timer("mudo.revenue.report.generate.duration"));
        } catch (RuntimeException metricError) {
            log.warn("event=revenue_report_metric_기록_실패 reason={}", metricError.getMessage(), metricError);
        }
    }

    /**
     * 원장(ACADEMY:OWNER) 계정이 아직 없으면(부트스트랩 전 등) 조용히 건너뛴다 — 알림 발송은
     * 리포트 생성의 성공 여부에 영향을 주지 않는다.
     */
    private void publishGeneratedEvent(RevenueReport saved) {
        academyOwnerLookupPort.findAcademyOwnerUserId().ifPresent(ownerUserId -> eventPublisher.publishEvent(
                new RevenueReportGeneratedEvent(ownerUserId, saved.getId(), saved.getTargetMonth())));
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
