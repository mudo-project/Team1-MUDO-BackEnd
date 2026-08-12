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
 *
 * <p>실행할 때마다 직전 달뿐 아니라 그 이전 {@link #RECONCILE_MONTHS}개월도 함께 재시도한다
 * — AI 서버 장애 등으로 특정 달 생성이 실패하면 그 실패가 다음 달까지 방치되지 않도록 하는
 * 최소한의 재처리 장치다. {@code generate()}는 이미 존재하는 달이면 저비용 SELECT 한 번으로
 * 스킵하므로, 이미 완료된 달을 매번 다시 확인하는 비용은 무시할 만하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevenueReportBatchScheduler {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int RECONCILE_MONTHS = 3;

    private final GenerateRevenueReportUseCase generateRevenueReportUseCase;

    @Scheduled(cron = "${app.revenue-report.batch-cron:0 30 0 1 * *}", zone = "Asia/Seoul")
    public void generateMonthlyReport() {
        LocalDate latestTargetMonth = LocalDate.now(SEOUL).minusMonths(1).withDayOfMonth(1);
        for (int monthsBack = 0; monthsBack < RECONCILE_MONTHS; monthsBack++) {
            generateForMonth(latestTargetMonth.minusMonths(monthsBack));
        }
    }

    private void generateForMonth(LocalDate targetMonth) {
        log.info("event=revenue_report_batch_시작 targetMonth={}", targetMonth);
        try {
            generateRevenueReportUseCase.generate(targetMonth);
            log.info("event=revenue_report_batch_완료 targetMonth={}", targetMonth);
        } catch (RuntimeException e) {
            // 배치 실패가 앱을 죽이면 안 되고, 한 달의 실패가 다른 달 재처리를 막아서도 안 된다
            // — 로그만 남기고 다음 달(재처리 루프의 다음 반복 또는 다음 스케줄)을 기다린다.
            log.error("event=revenue_report_batch_실패 targetMonth={}", targetMonth, e);
        }
    }
}
