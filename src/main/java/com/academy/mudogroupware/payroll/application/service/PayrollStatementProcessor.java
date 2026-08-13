package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.global.infrastructure.observability.InstanceMetadataProperties;
import com.academy.mudogroupware.payroll.application.event.PayrollConfirmedEvent;
import com.academy.mudogroupware.payroll.application.event.PayrollStatementRetryRequestedEvent;
import com.academy.mudogroupware.payroll.application.port.out.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollStatementProcessor {
  private final PayrollService payrollService;
  private final PayrollStatementRenderer renderer;
  private final PayrollStatementStoragePort storage;
  private final PayrollStatementPort statements;
  private final InstanceMetadataProperties instanceMetadata;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onConfirmed(PayrollConfirmedEvent event) {
    generate(event.payrollId());
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onRetry(PayrollStatementRetryRequestedEvent event) {
    generate(event.payrollId());
  }

  private void generate(Long payrollId) {
    try {
      var detail = payrollService.get(payrollId);
      byte[] pdf = renderer.render(detail);
      String key = "tenants/%s/payroll-statements/%04d/%02d/%s.pdf".formatted(
          instanceMetadata.getTenantId(), detail.yearMonth().getYear(),
          detail.yearMonth().getMonthValue(), UUID.randomUUID());
      storage.upload(key, pdf, "application/pdf");
      String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf));
      statements.markReady(payrollId, key, pdf.length, checksum, LocalDateTime.now());
      log.info("event=payroll_statement_generation_완료 payrollId={}", payrollId);
    } catch (Exception e) {
      statements.markFailed(payrollId, e.getMessage());
      log.error("event=payroll_statement_generation_실패 payrollId={}, errorType={}",
          payrollId, e.getClass().getSimpleName());
    }
  }
}
