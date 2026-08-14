package com.academy.mudogroupware.payroll.application.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.DeliveryStatus.SENDING;
import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.StatementStatus.READY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailRequestedEvent;
import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailWorkChangedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.DeliveryData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender.FailureType;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender.SendException;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort.StatementData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementStoragePort;
import com.academy.mudogroupware.payroll.domain.model.Payroll;
import com.academy.mudogroupware.planquota.application.service.CurrentPlanProvider;
import com.academy.mudogroupware.planquota.domain.model.Plan;
import com.academy.mudogroupware.planquota.domain.model.PlanLimits;
import com.academy.mudogroupware.resourceusage.application.command.RecordMailUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageQueryPort;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;
import com.academy.mudogroupware.resourceusage.domain.model.ResourceUsageType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PayrollStatementEmailProcessorTest {
  @Mock PayrollStatementEmailExecutor executor;
  @Mock PayrollRepository payrolls;
  @Mock PayrollStatementPort statements;
  @Mock PayrollStatementStoragePort storage;
  @Mock PayrollStatementEmailSender sender;
  @Mock ApplicationEventPublisher events;
  @Mock Payroll payroll;
  @Mock ResourceUsageQueryPort resourceUsageQueryPort;
  @Mock ResourceUsageRecorder resourceUsageRecorder;
  @Mock CurrentPlanProvider currentPlanProvider;
  private PayrollStatementEmailProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new PayrollStatementEmailProcessor(executor, payrolls, statements, storage, sender,
        new PayrollStatementEmailPolicy(20, 3, Duration.ofMinutes(1), Duration.ofMinutes(30),
            Duration.ofMinutes(10), Duration.ofMinutes(5)),
        events, resourceUsageQueryPort, resourceUsageRecorder, currentPlanProvider);
  }

  @Test
  void Mailgun_응답_ID를_SENT_이력에_저장한다() {
    allowProcessing(delivery(1));
    when(sender.send(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenReturn(PayrollStatementEmailSender.SendResult.success("<message@mailgun>"));

    processor.processPending(30L);

    verify(executor).sent(30L, "<message@mailgun>");
    verify(resourceUsageRecorder).recordMailUsage(new RecordMailUsageCommand("payroll-statement", 1L));
  }

  @Test
  void 명확한_일시_오류는_재시도를_예약한다() {
    allowProcessing(delivery(1));
    when(sender.send(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenThrow(new SendException(FailureType.RETRYABLE, "MAILGUN_RATE_LIMITED", null));

    processor.processPending(30L);

    verify(executor).retry(eq(30L), eq("MAILGUN_RATE_LIMITED"), anyString(),
        any(LocalDateTime.class));
    verify(executor, never()).unknown(any(), anyString(), anyString());
  }

  @Test
  void 접수_여부가_불명확하면_재발송하지_않고_UNKNOWN으로_전환한다() {
    allowProcessing(delivery(1));
    when(sender.send(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenThrow(new SendException(FailureType.UNKNOWN, "MAILGUN_RESULT_UNKNOWN", null));

    processor.processPending(30L);

    verify(executor).unknown(30L, "MAILGUN_RESULT_UNKNOWN",
        "Mailgun 접수 여부를 확인할 수 없어 대사가 필요합니다.");
    verify(executor, never()).retry(any(), anyString(), anyString(), any());
  }

  @Test
  void 최대_시도_횟수를_채우면_최종_실패한다() {
    allowProcessing(delivery(3));
    when(sender.send(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenThrow(new SendException(FailureType.RETRYABLE, "MAILGUN_RATE_LIMITED", null));

    processor.processPending(30L);

    verify(executor).failed(30L, "RETRY_EXHAUSTED", "이메일 발송 최대 재시도 횟수를 초과했습니다.");
    verify(executor, never()).retry(any(), anyString(), anyString(), any());
  }

  @Test
  void Mailgun_접수_후_SENT_기록이_실패해도_재발송을_예약하지_않는다() {
    allowProcessing(delivery(1));
    when(sender.send(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenReturn(PayrollStatementEmailSender.SendResult.success("<message@mailgun>"));
    doThrow(new IllegalStateException("database unavailable"))
        .when(executor).sent(30L, "<message@mailgun>");

    processor.processPending(30L);

    verify(executor, never()).retry(any(), anyString(), anyString(), any());
    verify(executor, never()).unknown(any(), anyString(), anyString());
  }

  @Test
  void 즉시발송이_끝나면_다음_작업시각을_다시_계산하도록_알린다() {
    when(executor.claim(30L)).thenReturn(Optional.empty());

    processor.onRequested(new PayrollStatementEmailRequestedEvent(30L));

    verify(events).publishEvent(any(PayrollStatementEmailWorkChangedEvent.class));
  }

  @Test
  void 메일_한도를_초과하면_발송하지_않고_건너뛴다() {
    when(executor.claim(30L)).thenReturn(Optional.of(delivery(1)));
    when(currentPlanProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.FREE));
    when(resourceUsageQueryPort.sumByTypeAndPeriod(eq(ResourceUsageType.MAIL), any(), any()))
        .thenReturn(100L);

    processor.processPending(30L);

    verify(executor).skipped(30L, "PLAN_MAIL_LIMIT_EXCEEDED");
    verifyNoInteractions(sender);
  }

  @Test
  void 동시에_처리되는_두_발송_중_한도를_넘기는_한_건은_건너뛴다() throws Exception {
    java.util.concurrent.atomic.AtomicLong mailUsage = new java.util.concurrent.atomic.AtomicLong(99L);
    when(executor.claim(30L)).thenReturn(Optional.of(delivery(1)));
    when(executor.claim(31L)).thenReturn(Optional.of(deliveryWithId(31L, 1)));
    when(currentPlanProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.FREE));
    when(resourceUsageQueryPort.sumByTypeAndPeriod(eq(ResourceUsageType.MAIL), any(), any()))
        .thenAnswer(invocation -> mailUsage.get());
    org.mockito.Mockito.doAnswer(invocation -> {
      mailUsage.incrementAndGet();
      return null;
    }).when(resourceUsageRecorder).recordMailUsage(any());
    when(statements.findById(20L)).thenReturn(Optional.of(new StatementData(
        20L, 1L, READY, "statement.pdf", "application/pdf", 10L, "checksum",
        LocalDateTime.now(), null)));
    when(payrolls.findById(1L)).thenReturn(Optional.of(payroll));
    when(payroll.getYearMonth()).thenReturn(YearMonth.of(2026, 8));
    when(storage.download("statement.pdf")).thenReturn(new byte[] {1});
    when(sender.send(anyString(), anyString(), anyString(), anyString(), any(), anyString()))
        .thenReturn(PayrollStatementEmailSender.SendResult.success("<message@mailgun>"));

    java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
    java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var first = pool.submit(() -> {
        ready.countDown();
        awaitUninterruptibly(start);
        processor.processPending(30L);
      });
      var second = pool.submit(() -> {
        ready.countDown();
        awaitUninterruptibly(start);
        processor.processPending(31L);
      });
      ready.await();
      start.countDown();
      first.get(10, java.util.concurrent.TimeUnit.SECONDS);
      second.get(10, java.util.concurrent.TimeUnit.SECONDS);
    } finally {
      pool.shutdown();
    }

    verify(sender, org.mockito.Mockito.times(1))
        .send(anyString(), anyString(), anyString(), anyString(), any(), anyString());
    verify(executor, org.mockito.Mockito.times(1))
        .skipped(org.mockito.ArgumentMatchers.anyLong(), eq("PLAN_MAIL_LIMIT_EXCEEDED"));
    assertThat(mailUsage.get()).isEqualTo(100L);
  }

  private static void awaitUninterruptibly(java.util.concurrent.CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }

  private void allowProcessing(DeliveryData delivery) {
    when(executor.claim(30L)).thenReturn(Optional.of(delivery));
    when(currentPlanProvider.currentLimits()).thenReturn(PlanLimits.of(Plan.PAID));
    when(resourceUsageQueryPort.sumByTypeAndPeriod(eq(ResourceUsageType.MAIL), any(), any()))
        .thenReturn(0L);
    when(statements.findById(20L)).thenReturn(Optional.of(new StatementData(
        20L, 1L, READY, "statement.pdf", "application/pdf", 10L, "checksum",
        LocalDateTime.now(), null)));
    when(payrolls.findById(1L)).thenReturn(Optional.of(payroll));
    when(payroll.getYearMonth()).thenReturn(YearMonth.of(2026, 8));
    when(storage.download("statement.pdf")).thenReturn(new byte[] {1});
  }

  private DeliveryData delivery(int attempts) {
    return deliveryWithId(30L, attempts);
  }

  private DeliveryData deliveryWithId(long deliveryId, int attempts) {
    return new DeliveryData(deliveryId, null, 20L, 1L, 10L, "staff@example.com", SENDING,
        null, null, "delivery-token-" + deliveryId, null, 99L, LocalDateTime.now(), LocalDateTime.now(),
        null, null, null, attempts, null, LocalDateTime.now(), null);
  }
}
