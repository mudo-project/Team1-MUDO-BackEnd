package com.academy.mudogroupware.payroll.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PayrollStatementEmailRecoveryServiceTest {

  @Test
  void 오래된_SENDING은_실패가_아니라_UNKNOWN으로_복구한다() {
    PayrollStatementDeliveryPort deliveries = mock(PayrollStatementDeliveryPort.class);
    PayrollStatementEmailRecoveryService service = new PayrollStatementEmailRecoveryService(
        deliveries);
    when(deliveries.markStaleSendingUnknown(any(), any())).thenReturn(2);

    assertThat(service.recover(Duration.ofMinutes(15))).isEqualTo(2);

    verify(deliveries).markStaleSendingUnknown(any(), any());
  }
}
