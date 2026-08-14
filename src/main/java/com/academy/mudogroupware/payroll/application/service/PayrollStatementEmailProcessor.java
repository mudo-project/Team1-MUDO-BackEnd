package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailRequestedEvent;
import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailWorkChangedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.DeliveryData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender.SendException;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollStatementEmailProcessor {
  private final PayrollStatementEmailExecutor executor;
  private final PayrollRepository payrolls;
  private final PayrollStatementPort statements;
  private final PayrollStatementStoragePort storage;
  private final PayrollStatementEmailSender sender;
  private final PayrollStatementEmailPolicy policy;
  private final ApplicationEventPublisher events;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRequested(PayrollStatementEmailRequestedEvent event) {
    try {
      process(event.deliveryId());
    } finally {
      events.publishEvent(new PayrollStatementEmailWorkChangedEvent());
    }
  }

  public void processPending(Long deliveryId) {
    process(deliveryId);
  }

  private void process(Long deliveryId) {
    var delivery = executor.claim(deliveryId);
    if (delivery.isEmpty()) return;
    var data = delivery.orElseThrow();
    log.info("event=payroll_statement_email_send_시작 deliveryId={}, attempt={}",
        deliveryId, data.attemptCount());
    PreparedEmail email;
    try {
      email = prepare(data);
    } catch (Exception e) {
      scheduleRetryOrFail(data, "EMAIL_PREPARATION_FAILED", "급여명세서 발송 준비에 실패했습니다.");
      log.warn("event=payroll_statement_email_send_실패 deliveryId={}, errorType={}",
          deliveryId, e.getClass().getSimpleName());
      return;
    }

    PayrollStatementEmailSender.SendResult result;
    try {
      result = sender.send(data.recipientEmail(), email.subject(), email.body(),
          email.attachmentName(), email.attachment(), data.deliveryToken());
    } catch (SendException e) {
      handleSendFailure(data, e);
      log.warn("event=payroll_statement_email_send_실패 deliveryId={}, errorType={}, failureType={}",
          deliveryId, e.getClass().getSimpleName(), e.failureType());
      return;
    } catch (RuntimeException e) {
      executor.unknown(data.id(), "MAILGUN_RESULT_UNKNOWN",
          "Mailgun 접수 여부를 확인할 수 없어 대사가 필요합니다.");
      log.warn("event=payroll_statement_email_send_실패 deliveryId={}, errorType={}, failureType=UNKNOWN",
          deliveryId, e.getClass().getSimpleName());
      return;
    }

    try {
      if (result.sent()) executor.sent(data.id(), result.providerMessageId());
      else executor.skipped(data.id(), result.skipCode());
      log.info("event=payroll_statement_email_send_완료 deliveryId={}", deliveryId);
    } catch (RuntimeException e) {
      // 외부 접수 이후 상태 기록 실패는 재발송하지 않는다. SENDING 만료 복구가 UNKNOWN으로 전환한다.
      log.warn("event=payroll_statement_email_state_record_실패 deliveryId={}, errorType={}",
          deliveryId, e.getClass().getSimpleName());
    }
  }

  private PreparedEmail prepare(DeliveryData data) {
    var statement = statements.findById(data.statementId()).orElseThrow();
    var month = payrolls.findById(data.payrollId()).orElseThrow().getYearMonth();
    byte[] pdf = storage.download(statement.objectKey());
    String label = month.getYear() + "년 " + month.getMonthValue() + "월 급여명세서";
    return new PreparedEmail("[MUDO] " + label, "급여명세서를 첨부파일로 전달드립니다.",
        label + ".pdf", pdf);
  }

  private void handleSendFailure(DeliveryData data, SendException exception) {
    switch (exception.failureType()) {
      case RETRYABLE -> scheduleRetryOrFail(data, exception.failureCode(),
          "Mailgun이 발송 요청을 거절하여 재시도를 예약했습니다.");
      case PERMANENT -> executor.failed(data.id(), exception.failureCode(),
          "Mailgun이 발송 요청을 영구적으로 거절했습니다.");
      case UNKNOWN -> executor.unknown(data.id(), exception.failureCode(),
          "Mailgun 접수 여부를 확인할 수 없어 대사가 필요합니다.");
    }
  }

  private void scheduleRetryOrFail(DeliveryData data, String code, String reason) {
    if (data.attemptCount() >= policy.maxAttempts()) {
      executor.failed(data.id(), "RETRY_EXHAUSTED", "이메일 발송 최대 재시도 횟수를 초과했습니다.");
      return;
    }
    executor.retry(data.id(), code, reason,
        java.time.LocalDateTime.now().plus(policy.retryDelay(data.attemptCount())));
  }

  private record PreparedEmail(
      String subject, String body, String attachmentName, byte[] attachment) {}
}
