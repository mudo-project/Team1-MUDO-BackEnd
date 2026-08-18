package com.academy.mudogroupware.payroll.application.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PayrollStatementEmailPolicy {
  private final int dispatchBatchSize;
  private final int maxAttempts;
  private final Duration retryBaseDelay;
  private final Duration retryMaxDelay;
  private final Duration reconcileAfter;
  private final Duration reconcileCooldown;

  public PayrollStatementEmailPolicy(
      @Value("${app.mail.dispatch-batch-size:20}") int dispatchBatchSize,
      @Value("${app.mail.max-attempts:3}") int maxAttempts,
      @Value("${app.mail.retry-base-delay:PT1M}") Duration retryBaseDelay,
      @Value("${app.mail.retry-max-delay:PT30M}") Duration retryMaxDelay,
      @Value("${app.mail.reconcile-after:PT10M}") Duration reconcileAfter,
      @Value("${app.mail.reconcile-cooldown:PT5M}") Duration reconcileCooldown) {
    if (dispatchBatchSize < 1 || maxAttempts < 1) {
      throw new IllegalArgumentException("mail dispatch batch size and max attempts must be positive");
    }
    this.dispatchBatchSize = dispatchBatchSize;
    this.maxAttempts = maxAttempts;
    this.retryBaseDelay = retryBaseDelay;
    this.retryMaxDelay = retryMaxDelay;
    this.reconcileAfter = reconcileAfter;
    this.reconcileCooldown = reconcileCooldown;
  }

  public int dispatchBatchSize() {
    return dispatchBatchSize;
  }

  public int maxAttempts() {
    return maxAttempts;
  }

  public Duration reconcileAfter() {
    return reconcileAfter;
  }

  public Duration reconcileCooldown() {
    return reconcileCooldown;
  }

  Duration retryDelay(int attemptCount) {
    long multiplier = 1L << Math.min(Math.max(attemptCount - 1, 0), 20);
    Duration calculated = retryBaseDelay.multipliedBy(multiplier);
    return calculated.compareTo(retryMaxDelay) > 0 ? retryMaxDelay : calculated;
  }
}
