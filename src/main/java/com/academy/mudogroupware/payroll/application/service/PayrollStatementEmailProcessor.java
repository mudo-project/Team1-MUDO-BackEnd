package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailRequestedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRequested(PayrollStatementEmailRequestedEvent event) {
    var delivery = executor.claim(event.deliveryId());
    if (delivery.isEmpty()) return;
    log.info("event=payroll_statement_email_send_시작 deliveryId={}", event.deliveryId());
    try {
      var data = delivery.orElseThrow();
      var statement = statements.findById(data.statementId()).orElseThrow();
      var month = payrolls.findById(data.payrollId()).orElseThrow().getYearMonth();
      byte[] pdf = storage.download(statement.objectKey());
      String label = month.getYear() + "년 " + month.getMonthValue() + "월 급여명세서";
      var result = sender.send(data.recipientEmail(), "[MUDO] " + label,
          "급여명세서를 첨부파일로 전달드립니다.", label + ".pdf", pdf,
          data.deliveryToken());
      if (result.sent()) executor.sent(data.id());
      else executor.skipped(data.id(), result.skipCode());
      log.info("event=payroll_statement_email_send_완료 deliveryId={}", event.deliveryId());
    } catch (Exception e) {
      executor.failed(event.deliveryId(), "EMAIL_SEND_FAILED", "급여명세서 이메일 발송에 실패했습니다.");
      log.error("event=payroll_statement_email_send_실패 deliveryId={}, errorType={}",
          event.deliveryId(), e.getClass().getSimpleName());
    }
  }
}
