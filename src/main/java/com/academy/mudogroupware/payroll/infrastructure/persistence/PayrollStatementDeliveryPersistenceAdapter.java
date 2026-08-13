package com.academy.mudogroupware.payroll.infrastructure.persistence;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.DeliveryStatus;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollStatementDeliveryPersistenceAdapter implements PayrollStatementDeliveryPort {
  private static final String DELIVERY_SELECT = "select d.* from payroll_statement_delivery d ";

  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;

  @Override
  public BatchData createBatch(YearMonth month, Long requestedBy, LocalDateTime requestedAt) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    namedJdbc.update("insert into payroll_statement_delivery_batch "
            + "(payroll_year_month, requested_by, requested_at) "
            + "values (:month, :requestedBy, :requestedAt)",
        new MapSqlParameterSource()
            .addValue("month", Date.valueOf(month.atDay(1)))
            .addValue("requestedBy", requestedBy)
            .addValue("requestedAt", Timestamp.valueOf(requestedAt)), keys);
    return new BatchData(keys.getKey().longValue(), month, requestedBy, requestedAt);
  }

  @Override
  public Optional<BatchData> findBatch(Long batchId) {
    try {
      return Optional.ofNullable(jdbc.queryForObject(
          "select * from payroll_statement_delivery_batch where batch_id=?", this::mapBatch,
          batchId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public DeliveryData create(Long batchId, Long payrollId, Long statementId, Long userId,
      String recipientEmail, String deliveryToken, Long requestedBy,
      LocalDateTime requestedAt, DeliveryStatus status,
      String failureCode, String failureReason) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    namedJdbc.update("insert into payroll_statement_delivery "
            + "(batch_id, payroll_id, statement_id, user_id, recipient_email, status, failure_code, "
            + "failure_reason, delivery_token, requested_by, requested_at) values "
            + "(:batchId, :payrollId, :statementId, :userId, :email, :status, :failureCode, "
            + ":failureReason, :token, :requestedBy, :requestedAt)",
        new MapSqlParameterSource()
            .addValue("batchId", batchId)
            .addValue("payrollId", payrollId)
            .addValue("statementId", statementId)
            .addValue("userId", userId)
            .addValue("email", recipientEmail)
            .addValue("status", status.name())
            .addValue("failureCode", failureCode)
            .addValue("failureReason", failureReason)
            .addValue("token", deliveryToken)
            .addValue("requestedBy", requestedBy)
            .addValue("requestedAt", Timestamp.valueOf(requestedAt)), keys);
    return findById(keys.getKey().longValue()).orElseThrow();
  }

  @Override
  public boolean existsBlocking(Long statementId) {
    Integer count = jdbc.queryForObject("select count(*) from payroll_statement_delivery "
        + "where statement_id=? and status in ('PENDING','SENDING','SENT','DELIVERED')",
        Integer.class, statementId);
    return count != null && count > 0;
  }

  @Override
  public Optional<DeliveryData> claim(Long deliveryId, LocalDateTime startedAt) {
    int changed = jdbc.update("update payroll_statement_delivery set status='SENDING', "
        + "sending_started_at=? where delivery_id=? and status='PENDING'", startedAt, deliveryId);
    return changed == 0 ? Optional.empty() : findById(deliveryId);
  }

  @Override
  public void markSent(Long deliveryId, LocalDateTime sentAt) {
    jdbc.update("update payroll_statement_delivery set status='SENT', sent_at=? "
        + "where delivery_id=? and status='SENDING'", sentAt, deliveryId);
  }

  @Override
  public void markSkipped(Long deliveryId, String code) {
    jdbc.update("update payroll_statement_delivery set status='SKIPPED', failure_code=?, "
        + "failure_reason=null where delivery_id=? and status='SENDING'", code, deliveryId);
  }

  @Override
  public void markFailed(Long deliveryId, String code, String reason, LocalDateTime failedAt) {
    jdbc.update("update payroll_statement_delivery set status='FAILED', failure_code=?, "
        + "failure_reason=?, failed_at=? where delivery_id=? and status in ('SENDING','SENT')",
        code, safe(reason), failedAt, deliveryId);
  }

  @Override
  public void markDelivered(String token, String messageId, LocalDateTime deliveredAt) {
    jdbc.update("update payroll_statement_delivery set status='DELIVERED', "
        + "mailgun_message_id=coalesce(?, mailgun_message_id), delivered_at=?, "
        + "failure_code=null, failure_reason=null, failed_at=null "
        + "where delivery_token=? and status in ('SENDING','SENT','FAILED')",
        messageId, deliveredAt, token);
  }

  @Override
  public void markPermanentFailure(String token, String messageId, String reason,
      LocalDateTime failedAt) {
    jdbc.update("update payroll_statement_delivery set status='FAILED', "
        + "mailgun_message_id=coalesce(?, mailgun_message_id), failure_code='PERMANENT_FAILURE', "
        + "failure_reason=?, failed_at=? where delivery_token=? "
        + "and status in ('SENDING','SENT')", messageId, safe(reason), failedAt, token);
  }

  @Override
  public int failStaleSending(LocalDateTime startedBefore, LocalDateTime failedAt) {
    return jdbc.update("update payroll_statement_delivery set status='FAILED', "
            + "failure_code='WORKER_TIMEOUT', failure_reason='발송 작업이 제한 시간 안에 완료되지 않았습니다.', "
            + "failed_at=? where status='SENDING' and sending_started_at<?",
        failedAt, startedBefore);
  }

  @Override
  public Optional<DeliveryData> findByToken(String token) {
    try {
      return Optional.ofNullable(jdbc.queryForObject(DELIVERY_SELECT
          + "where d.delivery_token=?", this::mapDelivery, token));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public List<DeliveryData> findByBatch(Long batchId, int limit, int offset) {
    return jdbc.query(DELIVERY_SELECT
        + "where d.batch_id=? order by d.delivery_id limit ? offset ?", this::mapDelivery,
        batchId, limit, offset);
  }

  @Override
  public long countByBatch(Long batchId) {
    Long count = jdbc.queryForObject(
        "select count(*) from payroll_statement_delivery where batch_id=?", Long.class, batchId);
    return count == null ? 0 : count;
  }

  @Override
  public List<StatusCount> countStatuses(Long batchId) {
    return jdbc.query("select status, count(*) as total from payroll_statement_delivery "
        + "where batch_id=? group by status",
        (rs, row) -> new StatusCount(DeliveryStatus.valueOf(rs.getString("status")),
            rs.getLong("total")), batchId);
  }

  private Optional<DeliveryData> findById(Long deliveryId) {
    try {
      return Optional.ofNullable(jdbc.queryForObject(DELIVERY_SELECT
          + "where d.delivery_id=?", this::mapDelivery, deliveryId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  private BatchData mapBatch(ResultSet rs, int row) throws SQLException {
    return new BatchData(rs.getLong("batch_id"), YearMonth.from(rs.getDate(
        "payroll_year_month").toLocalDate()), rs.getLong("requested_by"),
        rs.getTimestamp("requested_at").toLocalDateTime());
  }

  private DeliveryData mapDelivery(ResultSet rs, int row) throws SQLException {
    return new DeliveryData(rs.getLong("delivery_id"), nullableLong(rs, "batch_id"),
        nullableLong(rs, "statement_id"), rs.getLong("payroll_id"), rs.getLong("user_id"),
        rs.getString("recipient_email"), DeliveryStatus.valueOf(rs.getString("status")),
        rs.getString("failure_code"), rs.getString("failure_reason"),
        rs.getString("delivery_token"), rs.getString("mailgun_message_id"),
        rs.getLong("requested_by"), time(rs, "requested_at"),
        time(rs, "sending_started_at"), time(rs, "sent_at"), time(rs, "delivered_at"),
        time(rs, "failed_at"));
  }

  private Long nullableLong(ResultSet rs, String column) throws SQLException {
    return (Long) rs.getObject(column);
  }

  private LocalDateTime time(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toLocalDateTime();
  }

  private String safe(String reason) {
    if (reason == null || reason.isBlank()) return "알 수 없는 오류";
    return reason.substring(0, Math.min(1000, reason.length()));
  }
}
