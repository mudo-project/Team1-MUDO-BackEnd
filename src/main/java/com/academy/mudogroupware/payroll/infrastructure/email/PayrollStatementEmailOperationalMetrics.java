package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class PayrollStatementEmailOperationalMetrics {
  private final PayrollStatementDeliveryPort deliveries;
  private final AtomicLong pending = new AtomicLong();
  private final AtomicLong retryWait = new AtomicLong();
  private final AtomicLong unknown = new AtomicLong();
  private final AtomicLong oldestWaitingAge = new AtomicLong();
  private final AtomicLong retryAttempts = new AtomicLong();
  private final Counter reconciliationFailures;

  public PayrollStatementEmailOperationalMetrics(
      PayrollStatementDeliveryPort deliveries, MeterRegistry registry) {
    this.deliveries = deliveries;
    Gauge.builder("mudo.payroll.email.pending", pending, AtomicLong::get).register(registry);
    Gauge.builder("mudo.payroll.email.retry.wait", retryWait, AtomicLong::get).register(registry);
    Gauge.builder("mudo.payroll.email.unknown", unknown, AtomicLong::get).register(registry);
    Gauge.builder("mudo.payroll.email.oldest.waiting.age.seconds",
        oldestWaitingAge, AtomicLong::get).register(registry);
    Gauge.builder("mudo.payroll.email.retry.attempts", retryAttempts, AtomicLong::get)
        .register(registry);
    reconciliationFailures = Counter.builder("mudo.payroll.email.reconciliation.failures")
        .register(registry);
  }

  public void refresh() {
    var snapshot = deliveries.getOperationalSnapshot(LocalDateTime.now());
    pending.set(snapshot.pendingCount());
    retryWait.set(snapshot.retryWaitCount());
    unknown.set(snapshot.unknownCount());
    oldestWaitingAge.set(snapshot.oldestWaitingAgeSeconds());
    retryAttempts.set(snapshot.retryAttemptCount());
  }

  public void recordReconciliationFailures(int count) {
    if (count > 0) reconciliationFailures.increment(count);
  }
}
