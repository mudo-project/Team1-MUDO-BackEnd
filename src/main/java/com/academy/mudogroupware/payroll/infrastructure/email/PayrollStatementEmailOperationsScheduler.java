package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollStatementEmailOperationsScheduler {
  private final PayrollStatementEmailReconciliationService reconciliationService;
  private final PayrollStatementEmailOperationalMetrics metrics;

  @Scheduled(fixedDelayString = "${app.mail.reconcile-interval-ms:300000}")
  public void reconcile() {
    try {
      var result = reconciliationService.reconcile();
      metrics.recordReconciliationFailures(result.failureCount());
    } catch (RuntimeException e) {
      metrics.recordReconciliationFailures(1);
      log.warn("event=payroll_statement_email_reconcile_실패 errorType={}",
          e.getClass().getSimpleName());
    }
  }

  @Scheduled(fixedDelayString = "${app.mail.metrics-interval-ms:60000}")
  public void refreshMetrics() {
    metrics.refresh();
  }
}
