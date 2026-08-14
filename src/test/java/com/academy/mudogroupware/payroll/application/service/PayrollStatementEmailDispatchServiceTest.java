package com.academy.mudogroupware.payroll.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayrollStatementEmailDispatchServiceTest {
  @Mock PayrollStatementDeliveryPort deliveries;
  @Mock PayrollStatementEmailProcessor processor;

  @Test
  void 이벤트가_없어도_영속된_발송_대상을_다시_처리한다() {
    var policy = new PayrollStatementEmailPolicy(20, 3, Duration.ofMinutes(1),
        Duration.ofMinutes(30), Duration.ofMinutes(10), Duration.ofMinutes(5));
    var service = new PayrollStatementEmailDispatchService(deliveries, processor, policy);
    when(deliveries.findDispatchableIds(any(), anyInt()))
        .thenReturn(List.of(10L, 11L));

    assertThat(service.dispatch()).isEqualTo(2);

    verify(processor).processPending(10L);
    verify(processor).processPending(11L);
  }
}
