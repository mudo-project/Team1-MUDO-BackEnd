package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailStatusPort;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollStatementEmailReconciliationService {
  private final PayrollStatementDeliveryPort deliveries;
  private final PayrollStatementEmailStatusPort providerStatuses;
  private final PayrollStatementEmailPolicy policy;
  private final Clock clock;

  public ReconciliationResult reconcile() {
    LocalDateTime now = LocalDateTime.now(clock);
    var candidates = deliveries.findReconciliationCandidates(
        now.minus(policy.reconcileAfter()), now.minus(policy.reconcileCooldown()),
        policy.dispatchBatchSize());
    int failures = 0;
    for (var delivery : candidates) {
      try {
        var provider = providerStatuses.find(delivery.mailgunMessageId(),
            delivery.deliveryToken(), delivery.requestedAt());
        apply(delivery, provider, now);
      } catch (RuntimeException e) {
        failures++;
        deliveries.markReconciled(delivery.id(), now);
        log.warn("event=payroll_statement_email_reconcile_실패 deliveryId={}, errorType={}",
            delivery.id(), e.getClass().getSimpleName());
      }
    }
    return new ReconciliationResult(candidates.size(), failures);
  }

  private void apply(PayrollStatementDeliveryPort.DeliveryData delivery,
      PayrollStatementEmailStatusPort.ProviderDeliveryStatus provider, LocalDateTime now) {
    LocalDateTime occurredAt = provider.occurredAt() == null ? now : provider.occurredAt();
    switch (provider.status()) {
      case DELIVERED -> deliveries.markDelivered(
          delivery.deliveryToken(), provider.messageId(), occurredAt);
      case PERMANENT_FAILURE -> deliveries.markPermanentFailure(
          delivery.deliveryToken(), provider.messageId(), provider.reason(), occurredAt);
      case ACCEPTED, TEMPORARY_FAILURE -> deliveries.markSent(
          delivery.id(), provider.messageId(), occurredAt);
      case NOT_FOUND -> {
        // 조회 부재만으로 미접수를 단정할 수 없으므로 UNKNOWN을 자동 재발송하지 않는다.
      }
    }
    deliveries.markReconciled(delivery.id(), now);
  }

  public record ReconciliationResult(int processedCount, int failureCount) {}
}
