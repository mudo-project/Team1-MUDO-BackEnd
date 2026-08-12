package com.academy.mudogroupware.revenuereport.infrastructure.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.revenuereport.application.usecase.GenerateRevenueReportUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매달 1일 새벽, 직전 달 데이터를 기준으로 리포트를 생성한다. MUDO는 단일 테넌트라
 * 짐짝처럼 여러 대상을 순회할 필요 없이 리포트 하나만 생성한다.
 * 크론값은 프로퍼티로 분리해서 로컬에서 직접 바꿔가며 배치 발동 자체를 테스트할 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevenueReportBatchScheduler {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final GenerateRevenueReportUseCase generateRevenueReportUseCase;

    @Scheduled(cron = "${app.revenue-report.batch-cron}", zone = "Asia/Seoul")
    public void generateMonthlyReport() {
        LocalDate targetMonth = LocalDate.now(SEOUL).minusMonths(1).withDayOfMonth(1);
        log.info("event=revenue_report_batch_시작 targetMonth={}", targetMonth);
        try {
            generateRevenueReportUseCase.generate(targetMonth);
            log.info("event=revenue_report_batch_완료 targetMonth={}", targetMonth);
        } catch (RuntimeException e) {
            // 배치 실패가 앱을 죽이면 안 된다 — 로그만 남기고 다음 스케줄을 기다린다.
            log.error("event=revenue_report_batch_실패 targetMonth={}", targetMonth, e);
        }
    }
}
