package com.academy.mudogroupware.payroll.application.port.out;

import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.StatementStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface PayrollStatementPort {
  StatementData createPendingIfAbsent(Long payrollId);
  Optional<StatementData> findByPayrollId(Long payrollId);
  Map<Long, StatementData> findByPayrollIds(Set<Long> payrollIds);
  Optional<StatementData> findByPayrollIdForUpdate(Long payrollId);
  Map<Long, StatementData> findByPayrollIdsForUpdate(Set<Long> payrollIds);
  Optional<StatementData> findById(Long statementId);
  Optional<StatementData> markPendingIfFailed(Long payrollId);
  void markReady(Long payrollId, String objectKey, long fileSize, String checksum,
      LocalDateTime generatedAt);
  void markFailed(Long payrollId, String reason);

  record StatementData(Long id, Long payrollId, StatementStatus status, String objectKey,
      String contentType, Long fileSize, String checksum, LocalDateTime generatedAt,
      String failureReason) {}
}
