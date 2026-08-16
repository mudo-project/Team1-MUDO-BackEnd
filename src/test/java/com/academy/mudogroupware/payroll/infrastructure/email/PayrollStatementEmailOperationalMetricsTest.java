package com.academy.mudogroupware.payroll.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.OperationalSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class PayrollStatementEmailOperationalMetricsTest {

  @Test
  void 발송_대기와_재시도_대사_지표를_갱신한다() {
    PayrollStatementDeliveryPort deliveries = mock(PayrollStatementDeliveryPort.class);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    var metrics = new PayrollStatementEmailOperationalMetrics(deliveries, registry);
    when(deliveries.getOperationalSnapshot(any()))
        .thenReturn(new OperationalSnapshot(2, 3, 1, 400, 5));

    metrics.refresh();
    metrics.recordReconciliationFailures(2);

    assertThat(registry.get("mudo.payroll.email.pending").gauge().value()).isEqualTo(2);
    assertThat(registry.get("mudo.payroll.email.retry.wait").gauge().value()).isEqualTo(3);
    assertThat(registry.get("mudo.payroll.email.unknown").gauge().value()).isEqualTo(1);
    assertThat(registry.get("mudo.payroll.email.oldest.waiting.age.seconds").gauge().value())
        .isEqualTo(400);
    assertThat(registry.get("mudo.payroll.email.retry.attempts").gauge().value()).isEqualTo(5);
    assertThat(registry.get("mudo.payroll.email.reconciliation.failures").counter().count())
        .isEqualTo(2);
  }
}
