package com.academy.mudogroupware.payroll.application.service;

import static com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailStatusPort.ProviderStatus.DELIVERED;
import static com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailStatusPort.ProviderStatus.NOT_FOUND;
import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.DeliveryStatus.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.DeliveryData;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailStatusPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailStatusPort.ProviderDeliveryStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayrollStatementEmailReconciliationServiceTest {
  @Mock PayrollStatementDeliveryPort deliveries;
  @Mock PayrollStatementEmailStatusPort statuses;
  private PayrollStatementEmailReconciliationService service;

  @BeforeEach
  void setUp() {
    service = new PayrollStatementEmailReconciliationService(deliveries, statuses,
        new PayrollStatementEmailPolicy(20, 3, Duration.ofMinutes(1), Duration.ofMinutes(30),
            Duration.ofMinutes(10), Duration.ofMinutes(5)));
  }

  @Test
  void 유실된_delivered_Webhook을_Mailgun_조회_결과로_복구한다() {
    DeliveryData delivery = delivery();
    LocalDateTime deliveredAt = LocalDateTime.of(2026, 8, 14, 10, 0);
    when(deliveries.findReconciliationCandidates(any(), any(), anyInt()))
        .thenReturn(List.of(delivery));
    when(statuses.find(null, "delivery-token", delivery.requestedAt()))
        .thenReturn(new ProviderDeliveryStatus(DELIVERED, "message-id", deliveredAt, null));

    var result = service.reconcile();

    assertThat(result.failureCount()).isZero();
    verify(deliveries).markDelivered("delivery-token", "message-id", deliveredAt);
  }

  @Test
  void Mailgun에서_찾지_못한_UNKNOWN은_자동_재발송하지_않는다() {
    DeliveryData delivery = delivery();
    when(deliveries.findReconciliationCandidates(any(), any(), anyInt()))
        .thenReturn(List.of(delivery));
    when(statuses.find(null, "delivery-token", delivery.requestedAt()))
        .thenReturn(new ProviderDeliveryStatus(NOT_FOUND, null, null, null));

    service.reconcile();

    verify(deliveries).markReconciled(any(), any());
    verify(deliveries, never()).markRetry(any(), any(), any(), any());
  }

  private DeliveryData delivery() {
    LocalDateTime requestedAt = LocalDateTime.of(2026, 8, 14, 9, 0);
    return new DeliveryData(30L, null, 20L, 1L, 10L, "staff@example.com", UNKNOWN,
        "MAILGUN_RESULT_UNKNOWN", null, "delivery-token", null, 99L, requestedAt,
        LocalDateTime.now(), null, null, LocalDateTime.now(), 1, null,
        LocalDateTime.now(), null);
  }
}
