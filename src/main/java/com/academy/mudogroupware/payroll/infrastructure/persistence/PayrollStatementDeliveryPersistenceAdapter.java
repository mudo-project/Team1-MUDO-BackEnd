package com.academy.mudogroupware.payroll.infrastructure.persistence;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.DeliveryStatus;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  public Optional<DeliveryData> findBlocking(Long statementId) {
    try {
      return Optional.ofNullable(jdbc.queryForObject(DELIVERY_SELECT
          + "where d.statement_id=? "
          + "and d.status in ('PENDING','SENDING','RETRY_WAIT','UNKNOWN','SENT','DELIVERED') "
          + "order by d.delivery_id desc limit 1", this::mapDelivery, statementId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  @Override
  public Map<Long, DeliveryData> findBlockingByStatementIds(Set<Long> statementIds) {
    if (statementIds.isEmpty()) return Map.of();
    List<DeliveryData> rows = namedJdbc.query(DELIVERY_SELECT
            + "where d.statement_id in (:statementIds) "
            + "and d.status in ('PENDING','SENDING','RETRY_WAIT','UNKNOWN','SENT','DELIVERED') "
            + "order by d.statement_id, d.delivery_id desc",
        new MapSqlParameterSource("statementIds", statementIds), this::mapDelivery);
    Map<Long, DeliveryData> result = new LinkedHashMap<>();
    for (DeliveryData row : rows) result.putIfAbsent(row.statementId(), row);
    return result;
  }

  @Override
  public List<Long> findDispatchableIds(LocalDateTime now, int limit) {
    return jdbc.queryForList("select delivery_id from payroll_statement_delivery "
        + "where status='PENDING' or (status='RETRY_WAIT' and next_attempt_at<=?) "
        + "order by coalesce(next_attempt_at, requested_at), delivery_id limit ?",
        Long.class, now, limit);
  }

  @Override
  public Optional<DeliveryData> claim(Long deliveryId, LocalDateTime startedAt) {
    int changed = jdbc.update("update payroll_statement_delivery set status='SENDING', "
        + "sending_started_at=?, last_attempt_at=?, attempt_count=attempt_count+1, "
        + "failure_code=null, failure_reason=null, next_attempt_at=null where delivery_id=? "
        + "and (status='PENDING' or (status='RETRY_WAIT' and next_attempt_at<=?))",
        startedAt, startedAt, deliveryId, startedAt);
    return changed == 0 ? Optional.empty() : findById(deliveryId);
  }

  @Override
  public void markSent(Long deliveryId, String messageId, LocalDateTime sentAt) {
    jdbc.update("update payroll_statement_delivery set status='SENT', "
        + "mailgun_message_id=coalesce(?, mailgun_message_id), "
        + "sent_at=coalesce(sent_at, ?), next_attempt_at=null, "
        + "failure_code=null, failure_reason=null, failed_at=null "
        + "where delivery_id=? and status in ('SENDING','UNKNOWN','SENT')",
        messageId, sentAt, deliveryId);
  }

  @Override
  public void markRetry(Long deliveryId, String code, String reason,
      LocalDateTime nextAttemptAt) {
    jdbc.update("update payroll_statement_delivery set status='RETRY_WAIT', failure_code=?, "
        + "failure_reason=?, next_attempt_at=? where delivery_id=? and status='SENDING'",
        code, safe(reason), nextAttemptAt, deliveryId);
  }

  @Override
  public void markUnknown(Long deliveryId, String code, String reason, LocalDateTime failedAt) {
    jdbc.update("update payroll_statement_delivery set status='UNKNOWN', failure_code=?, "
        + "failure_reason=?, failed_at=?, next_attempt_at=null "
        + "where delivery_id=? and status='SENDING'", code, safe(reason), failedAt, deliveryId);
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
        + "where delivery_token=? "
        + "and status in ('SENDING','RETRY_WAIT','UNKNOWN','SENT','FAILED')",
        messageId, deliveredAt, token);
  }

  @Override
  public void markPermanentFailure(String token, String messageId, String reason,
      LocalDateTime failedAt) {
    jdbc.update("update payroll_statement_delivery set status='FAILED', "
        + "mailgun_message_id=coalesce(?, mailgun_message_id), failure_code='PERMANENT_FAILURE', "
        + "failure_reason=?, failed_at=? where delivery_token=? "
        + "and status in ('SENDING','RETRY_WAIT','UNKNOWN','SENT')",
        messageId, safe(reason), failedAt, token);
  }

  @Override
  public int markStaleSendingUnknown(LocalDateTime startedBefore, LocalDateTime failedAt) {
    return jdbc.update("update payroll_statement_delivery set status='UNKNOWN', "
            + "failure_code='WORKER_TIMEOUT_UNKNOWN', "
            + "failure_reason='발송 작업 제한 시간을 초과하여 외부 접수 여부 확인이 필요합니다.', "
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
  public List<DeliveryData> findReconciliationCandidates(
      LocalDateTime sentBefore, LocalDateTime reconciledBefore, int limit) {
    return jdbc.query(DELIVERY_SELECT
        + "where ((d.status='SENT' and d.sent_at<=?) or d.status='UNKNOWN') "
        + "and (d.last_reconciled_at is null or d.last_reconciled_at<=?) "
        + "order by coalesce(d.last_reconciled_at, d.requested_at), d.delivery_id limit ?",
        this::mapDelivery, sentBefore, reconciledBefore, limit);
  }

  @Override
  public Optional<LocalDateTime> findNextWakeupAt(
      Duration sendingTimeout, Duration reconcileAfter, Duration reconcileCooldown) {
    LocalDateTime next = jdbc.queryForObject("select min(next_at) from ("
            + "select min(requested_at) next_at from payroll_statement_delivery "
            + "where status='PENDING' union all "
            + "select min(next_attempt_at) next_at from payroll_statement_delivery "
            + "where status='RETRY_WAIT' union all "
            + "select min(timestampadd(second, ?, sending_started_at)) next_at "
            + "from payroll_statement_delivery where status='SENDING' union all "
            + "select min(case when status='SENT' then greatest("
            + "timestampadd(second, ?, sent_at), "
            + "coalesce(timestampadd(second, ?, last_reconciled_at), sent_at)) "
            + "else coalesce(timestampadd(second, ?, last_reconciled_at), "
            + "failed_at, requested_at) end) next_at "
            + "from payroll_statement_delivery where status in ('SENT','UNKNOWN')"
            + ") delivery_schedule",
        LocalDateTime.class, sendingTimeout.toSeconds(), reconcileAfter.toSeconds(),
        reconcileCooldown.toSeconds(), reconcileCooldown.toSeconds());
    return Optional.ofNullable(next);
  }

  @Override
  public void markReconciled(Long deliveryId, LocalDateTime reconciledAt) {
    jdbc.update("update payroll_statement_delivery set last_reconciled_at=? "
        + "where delivery_id=? and status in ('SENT','UNKNOWN')", reconciledAt, deliveryId);
  }

  @Override
  public OperationalSnapshot getOperationalSnapshot(LocalDateTime now) {
    return jdbc.queryForObject("select "
            + "sum(case when status='PENDING' then 1 else 0 end) pending_count, "
            + "sum(case when status='RETRY_WAIT' then 1 else 0 end) retry_count, "
            + "sum(case when status='UNKNOWN' then 1 else 0 end) unknown_count, "
            + "coalesce(timestampdiff(second, min(case when status='PENDING' "
            + "then requested_at end), ?), 0) oldest_age "
            + ", coalesce(sum(greatest(attempt_count-1, 0)), 0) retry_attempt_count "
            + "from payroll_statement_delivery",
        (rs, row) -> new OperationalSnapshot(rs.getLong("pending_count"),
            rs.getLong("retry_count"), rs.getLong("unknown_count"), rs.getLong("oldest_age"),
            rs.getLong("retry_attempt_count")),
        now);
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
        time(rs, "failed_at"), rs.getInt("attempt_count"), time(rs, "next_attempt_at"),
        time(rs, "last_attempt_at"), time(rs, "last_reconciled_at"));
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
