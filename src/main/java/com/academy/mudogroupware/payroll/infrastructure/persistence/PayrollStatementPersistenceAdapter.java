package com.academy.mudogroupware.payroll.infrastructure.persistence;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.StatementStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollStatementPersistenceAdapter implements PayrollStatementPort {
  private final JdbcTemplate jdbc;

  @Override public StatementData createPendingIfAbsent(Long payrollId) {
    jdbc.update("insert into payroll_statement (payroll_id, status) select ?, 'PENDING' "
        + "where not exists (select 1 from payroll_statement where payroll_id=?)", payrollId, payrollId);
    return findByPayrollId(payrollId).orElseThrow();
  }
  @Override public Optional<StatementData> findByPayrollId(Long payrollId) {
    try {
      return Optional.ofNullable(jdbc.queryForObject(
          "select * from payroll_statement where payroll_id=?", this::map, payrollId));
    } catch (EmptyResultDataAccessException e) { return Optional.empty(); }
  }
  @Override public Optional<StatementData> findByPayrollIdForUpdate(Long payrollId) {
    try {
      return Optional.ofNullable(jdbc.queryForObject(
          "select * from payroll_statement where payroll_id=? for update", this::map, payrollId));
    } catch (EmptyResultDataAccessException e) { return Optional.empty(); }
  }
  @Override public Optional<StatementData> findById(Long statementId) {
    try {
      return Optional.ofNullable(jdbc.queryForObject(
          "select * from payroll_statement where statement_id=?", this::map, statementId));
    } catch (EmptyResultDataAccessException e) { return Optional.empty(); }
  }
  @Override public Optional<StatementData> markPendingIfFailed(Long payrollId) {
    int changed = jdbc.update("update payroll_statement set status='PENDING', failure_reason=null "
        + "where payroll_id=? and status='FAILED'", payrollId);
    return changed == 0 ? Optional.empty() : findByPayrollId(payrollId);
  }
  @Override public void markReady(Long payrollId, String key, long size, String checksum,
      LocalDateTime generatedAt) {
    jdbc.update("update payroll_statement set status='READY', object_key=?, content_type='application/pdf', "
        + "file_size=?, checksum=?, generated_at=?, failure_reason=null where payroll_id=?",
        key, size, checksum, generatedAt, payrollId);
  }
  @Override public void markFailed(Long payrollId, String reason) {
    String safe = reason == null ? "알 수 없는 오류" : reason.substring(0, Math.min(1000, reason.length()));
    jdbc.update("update payroll_statement set status='FAILED', failure_reason=? where payroll_id=?",
        safe, payrollId);
  }
  private StatementData map(ResultSet rs, int row) throws SQLException {
    return new StatementData(rs.getLong("statement_id"), rs.getLong("payroll_id"),
        StatementStatus.valueOf(rs.getString("status")), rs.getString("object_key"),
        rs.getString("content_type"), (Long) rs.getObject("file_size"), rs.getString("checksum"),
        rs.getTimestamp("generated_at") == null ? null : rs.getTimestamp("generated_at").toLocalDateTime(),
        rs.getString("failure_reason"));
  }
}
