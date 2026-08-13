package com.academy.mudogroupware.payroll.application.port.out;

import com.academy.mudogroupware.payroll.domain.model.PayrollTypes.DeliveryStatus;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface PayrollStatementDeliveryPort {
  BatchData createBatch(YearMonth yearMonth, Long requestedBy, LocalDateTime requestedAt);
  Optional<BatchData> findBatch(Long batchId);
  DeliveryData create(Long batchId, Long payrollId, Long statementId, Long userId,
      String recipientEmail, String deliveryToken, Long requestedBy,
      LocalDateTime requestedAt, DeliveryStatus status,
      String failureCode, String failureReason);
  boolean existsBlocking(Long statementId);
  Optional<DeliveryData> claim(Long deliveryId, LocalDateTime startedAt);
  void markSent(Long deliveryId, LocalDateTime sentAt);
  void markSkipped(Long deliveryId, String code);
  void markFailed(Long deliveryId, String code, String reason, LocalDateTime failedAt);
  void markDelivered(String deliveryToken, String messageId, LocalDateTime deliveredAt);
  void markPermanentFailure(String deliveryToken, String messageId, String reason,
      LocalDateTime failedAt);
  int failStaleSending(LocalDateTime startedBefore, LocalDateTime failedAt);
  Optional<DeliveryData> findByToken(String deliveryToken);
  List<DeliveryData> findByBatch(Long batchId, int limit, int offset);
  long countByBatch(Long batchId);
  List<StatusCount> countStatuses(Long batchId);

  record BatchData(Long id, YearMonth yearMonth, Long requestedBy, LocalDateTime requestedAt) {}

  record DeliveryData(Long id, Long batchId, Long statementId, Long payrollId, Long userId,
      String recipientEmail, DeliveryStatus status, String failureCode, String failureReason,
      String deliveryToken, String mailgunMessageId, Long requestedBy,
      LocalDateTime requestedAt, LocalDateTime sendingStartedAt, LocalDateTime sentAt,
      LocalDateTime deliveredAt, LocalDateTime failedAt) {}

  record StatusCount(DeliveryStatus status, long count) {}
}
