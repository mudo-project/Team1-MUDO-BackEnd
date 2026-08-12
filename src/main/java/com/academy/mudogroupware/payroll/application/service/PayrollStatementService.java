package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementRetryRequestedEvent;
import com.academy.mudogroupware.payroll.application.port.out.*;
import com.academy.mudogroupware.payroll.domain.exception.*;
import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollStatementService {
  private static final long EXPIRES_SECONDS = 300;
  private final PayrollService payrollService;
  private final PayrollStatementPort statements;
  private final PayrollStatementStoragePort storage;
  private final ApplicationEventPublisher events;

  @Transactional(readOnly = true)
  public DownloadResult download(Long payrollId) {
    var payroll = payrollService.get(payrollId);
    if (payroll.status() != PayrollStatus.CONFIRMED) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_STATEMENT_NOT_READY);
    }
    var statement = statements.findByPayrollId(payrollId)
        .filter(s -> s.status() == StatementStatus.READY && s.objectKey() != null)
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_STATEMENT_NOT_READY));
    String name = "%d년 %d월 급여명세서.pdf".formatted(payroll.yearMonth().getYear(),
        payroll.yearMonth().getMonthValue());
    return new DownloadResult(statement.id(), payrollId, name,
        storage.generateDownloadUrl(statement.objectKey(), EXPIRES_SECONDS), EXPIRES_SECONDS);
  }

  @Transactional
  public StatementResult retry(Long payrollId) {
    var payroll = payrollService.get(payrollId);
    if (payroll.status() != PayrollStatus.CONFIRMED) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_STATEMENT_RETRY_NOT_ALLOWED);
    }
    var pending = statements.markPendingIfFailed(payrollId)
        .orElseThrow(() -> new PayrollException(
            PayrollErrorCode.PAYROLL_STATEMENT_RETRY_NOT_ALLOWED));
    events.publishEvent(new PayrollStatementRetryRequestedEvent(payrollId));
    return new StatementResult(pending.id(), pending.payrollId(), pending.status(), pending.failureReason());
  }

  public record DownloadResult(Long statementId, Long payrollId, String fileName,
      String downloadUrl, long expiresInSeconds) {}
  public record StatementResult(Long statementId, Long payrollId, StatementStatus status,
      String failureReason) {}
}
