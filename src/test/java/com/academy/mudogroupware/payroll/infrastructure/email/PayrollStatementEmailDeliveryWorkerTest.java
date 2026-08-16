package com.academy.mudogroupware.payroll.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailRequestedEvent;
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
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayrollStatementEmailDeliveryWorkerTest {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final Instant NOW = Instant.parse("2026-08-15T01:00:00Z");

  @Mock PayrollStatementEmailDispatchService dispatchService;
  @Mock PayrollStatementEmailRecoveryService recoveryService;
  @Mock PayrollStatementEmailReconciliationService reconciliationService;
  @Mock PayrollStatementDeliveryPort deliveries;
  @Mock PayrollStatementEmailOperationalMetrics metrics;
  @Mock PayrollEmailTaskScheduler scheduler;
  @Mock ScheduledFuture<?> scheduledFuture;
  private PayrollStatementEmailDeliveryWorker worker;

  @BeforeEach
  void setUp() {
    var properties = new PayrollEmailProperties("sender@example.com", "signing-key",
        Duration.ofMinutes(15));
    var policy = new PayrollStatementEmailPolicy(20, 3, Duration.ofMinutes(1),
        Duration.ofMinutes(30), Duration.ofMinutes(10), Duration.ofMinutes(5));
    worker = new PayrollStatementEmailDeliveryWorker(dispatchService, recoveryService,
        reconciliationService, deliveries, properties, policy, metrics, scheduler,
        Clock.fixed(NOW, SEOUL));
    doReturn(scheduledFuture).when(scheduler).schedule(any(), any());
    lenient().when(dispatchService.dispatch())
        .thenReturn(new PayrollStatementEmailDispatchService.DispatchResult(0, 0));
    lenient().when(reconciliationService.reconcile())
        .thenReturn(new PayrollStatementEmailReconciliationService.ReconciliationResult(0, 0));
  }

  @Test
  void 서버가_시작되면_한번_실행하고_남은_작업이_없으면_종료한다() {
    when(deliveries.findNextWakeupAt(any(), any(), any())).thenReturn(Optional.empty());

    worker.onApplicationReady();
    runFirstScheduledTask();

    verify(recoveryService).recover(Duration.ofMinutes(15));
    verify(dispatchService).dispatch();
    verify(reconciliationService).reconcile();
    verify(metrics).refresh();
    verify(scheduler).schedule(any(), any());
    verifyNoMoreInteractions(scheduler);
  }

  @Test
  void 미래_작업이_있으면_가장_빠른_시각에_단발_실행을_예약한다() {
    LocalDateTime next = LocalDateTime.ofInstant(NOW.plusSeconds(90), SEOUL);
    when(deliveries.findNextWakeupAt(any(), any(), any())).thenReturn(Optional.of(next));

    worker.kick();
    runFirstScheduledTask();

    ArgumentCaptor<Instant> times = ArgumentCaptor.forClass(Instant.class);
    verify(scheduler, times(2)).schedule(any(), times.capture());
    assertThat(times.getAllValues())
        .containsExactly(NOW, NOW.plusSeconds(90));
  }

  @Test
  void 발송요청이_커밋되면_worker를_즉시_실행하도록_예약한다() {
    worker.onRequested(new PayrollStatementEmailRequestedEvent(10L));

    verify(scheduler).schedule(any(), eq(NOW));
  }

  @Test
  void 여러번_kick해도_대기중인_즉시실행은_하나만_유지한다() {
    worker.kick();
    worker.kick();

    verify(scheduler).schedule(any(), any());
    verifyNoMoreInteractions(scheduler);
  }

  private void runFirstScheduledTask() {
    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    verify(scheduler).schedule(task.capture(), any());
    task.getValue().run();
  }
}
