package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailRequestedEvent;
import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailWorkChangedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailDispatchService;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailPolicy;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailReconciliationService;
import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailRecoveryService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PayrollStatementEmailDeliveryWorker {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final Duration FAILURE_RETRY_DELAY = Duration.ofMinutes(1);

  private final PayrollStatementEmailDispatchService dispatchService;
  private final PayrollStatementEmailRecoveryService recoveryService;
  private final PayrollStatementEmailReconciliationService reconciliationService;
  private final PayrollStatementDeliveryPort deliveries;
  private final PayrollEmailProperties properties;
  private final PayrollStatementEmailPolicy policy;
  private final PayrollStatementEmailOperationalMetrics metrics;
  private final PayrollEmailTaskScheduler scheduler;
  private final Clock clock;
  private final Object monitor = new Object();

  private ScheduledFuture<?> scheduledTask;
  private Instant scheduledAt;
  private boolean running;
  private boolean rerunRequested;

  @Autowired
  PayrollStatementEmailDeliveryWorker(
      PayrollStatementEmailDispatchService dispatchService,
      PayrollStatementEmailRecoveryService recoveryService,
      PayrollStatementEmailReconciliationService reconciliationService,
      PayrollStatementDeliveryPort deliveries,
      PayrollEmailProperties properties,
      PayrollStatementEmailPolicy policy,
      PayrollStatementEmailOperationalMetrics metrics,
      PayrollEmailTaskScheduler scheduler,
      Clock clock) {
    this.dispatchService = dispatchService;
    this.recoveryService = recoveryService;
    this.reconciliationService = reconciliationService;
    this.deliveries = deliveries;
    this.properties = properties;
    this.policy = policy;
    this.metrics = metrics;
    this.scheduler = scheduler;
    this.clock = clock;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    kick();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRequested(PayrollStatementEmailRequestedEvent event) {
    kick();
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onWorkChanged(PayrollStatementEmailWorkChangedEvent event) {
    kick();
  }

  public void kick() {
    schedule(clock.instant());
  }

  private void schedule(Instant startTime) {
    synchronized (monitor) {
      if (running) {
        rerunRequested = true;
        return;
      }
      if (scheduledTask != null && !scheduledTask.isDone()
          && !startTime.isBefore(scheduledAt)) {
        return;
      }
      if (scheduledTask != null) scheduledTask.cancel(false);
      scheduledAt = startTime;
      scheduledTask = scheduler.schedule(this::run, startTime);
    }
  }

  private void run() {
    synchronized (monitor) {
      scheduledTask = null;
      scheduledAt = null;
      running = true;
    }

    Instant next;
    try {
      next = nextWakeup(executeCycle());
    } catch (RuntimeException e) {
      log.warn("event=payroll_statement_email_worker_실패 errorType={}",
          e.getClass().getSimpleName());
      next = clock.instant().plus(FAILURE_RETRY_DELAY);
    }

    synchronized (monitor) {
      running = false;
      if (rerunRequested) {
        rerunRequested = false;
        next = clock.instant();
      }
    }
    if (next != null) schedule(next);
  }

  private boolean executeCycle() {
    boolean failed = false;
    int recovered = 0;
    int dispatched = 0;
    int reconciled = 0;
    try {
      recovered = recoveryService.recover(properties.sendingTimeout());
    } catch (RuntimeException e) {
      failed = true;
      log.warn("event=payroll_statement_email_recovery_실패 errorType={}",
          e.getClass().getSimpleName());
    }
    try {
      var result = dispatchService.dispatch();
      dispatched = result.processedCount();
      failed = failed || result.failureCount() > 0;
    } catch (RuntimeException e) {
      failed = true;
      log.warn("event=payroll_statement_email_dispatch_실패 errorType={}",
          e.getClass().getSimpleName());
    }
    try {
      var result = reconciliationService.reconcile();
      reconciled = result.processedCount();
      metrics.recordReconciliationFailures(result.failureCount());
    } catch (RuntimeException e) {
      failed = true;
      metrics.recordReconciliationFailures(1);
      log.warn("event=payroll_statement_email_reconcile_실패 errorType={}",
          e.getClass().getSimpleName());
    }
    try {
      metrics.refresh();
    } catch (RuntimeException e) {
      failed = true;
      log.warn("event=payroll_statement_email_metrics_실패 errorType={}",
          e.getClass().getSimpleName());
    }
    if (recovered + dispatched + reconciled > 0) {
      log.info(
          "event=payroll_statement_email_worker_완료 recoveredCount={}, dispatchedCount={}, reconciledCount={}",
          recovered, dispatched, reconciled);
    }
    return failed;
  }

  private Instant nextWakeup(boolean failed) {
    Instant now = clock.instant();
    Instant failureRetryAt = now.plus(FAILURE_RETRY_DELAY);
    try {
      Instant next = deliveries.findNextWakeupAt(properties.sendingTimeout(),
              policy.reconcileAfter(), policy.reconcileCooldown())
          .map(time -> time.atZone(SEOUL).toInstant())
          .orElse(null);
      if (failed && (next == null || next.isBefore(failureRetryAt))) return failureRetryAt;
      if (next == null) return null;
      return next.isBefore(now) ? now : next;
    } catch (RuntimeException e) {
      log.warn("event=payroll_statement_email_schedule_실패 errorType={}",
          e.getClass().getSimpleName());
      return failureRetryAt;
    }
  }
}
