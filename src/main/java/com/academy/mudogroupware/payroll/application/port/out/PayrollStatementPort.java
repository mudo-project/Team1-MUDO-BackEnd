package com.academy.mudogroupware.payroll.application.port.out;

import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.StatementStatus;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PayrollStatementPort {
  StatementData createPendingIfAbsent(Long payrollId);
  Optional<StatementData> findByPayrollId(Long payrollId);
  Optional<StatementData> markPendingIfFailed(Long payrollId);
  void markReady(Long payrollId, String objectKey, long fileSize, String checksum,
      LocalDateTime generatedAt);
  void markFailed(Long payrollId, String reason);

  record StatementData(Long id, Long payrollId, StatementStatus status, String objectKey,
      String contentType, Long fileSize, String checksum, LocalDateTime generatedAt,
      String failureReason) {}
}
