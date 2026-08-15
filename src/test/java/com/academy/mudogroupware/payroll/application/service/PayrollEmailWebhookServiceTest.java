package com.academy.mudogroupware.payroll.application.service;

import static com.academy.mudogroupware.payroll.domain.model.PayrollTypes.DeliveryStatus.SENT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailWorkChangedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmailWebhookVerifier;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.DeliveryData;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PayrollEmailWebhookServiceTest {
  @Mock PayrollEmailWebhookVerifier verifier;
  @Mock PayrollStatementDeliveryPort deliveries;
  @Mock ApplicationEventPublisher events;
  @InjectMocks PayrollEmailWebhookService service;

  @Test
  void 유효한_delivered_webhook은_수신서버_전달완료로_반영한다() {
    var command = command("delivered", null);
    when(verifier.verify("timestamp", "signature-token", "signature")).thenReturn(true);
    when(deliveries.findByToken("delivery-token")).thenReturn(Optional.of(delivery()));
    service.handle(command);
    verify(deliveries).markDelivered(eq("delivery-token"), eq("message-id"),
        any(LocalDateTime.class));
    verify(events).publishEvent(any(PayrollStatementEmailWorkChangedEvent.class));
  }

  @Test
  void 서명이_유효하지_않으면_상태를_변경하지_않는다() {
    var command = command("delivered", null);
    assertThatThrownBy(() -> service.handle(command)).isInstanceOf(PayrollException.class);
    verifyNoInteractions(deliveries);
  }

  @Test
  void 영구실패_webhook은_실패로_반영한다() {
    var command = command("failed", "permanent");
    when(verifier.verify("timestamp", "signature-token", "signature")).thenReturn(true);
    when(deliveries.findByToken("delivery-token")).thenReturn(Optional.of(delivery()));
    service.handle(command);
    verify(deliveries).markPermanentFailure(eq("delivery-token"), eq("message-id"),
        eq("mailbox unavailable"), any(LocalDateTime.class));
  }

  private PayrollEmailWebhookService.WebhookCommand command(String event, String severity) {
    return new PayrollEmailWebhookService.WebhookCommand("timestamp", "signature-token",
        "signature", event, severity, 1786460400, "delivery-token", "message-id",
        "mailbox unavailable");
  }

  private DeliveryData delivery() {
    return new DeliveryData(1L, null, 2L, 3L, 4L, "staff@example.com", SENT,
        null, null, "delivery-token", null, 5L, LocalDateTime.now(), null,
        LocalDateTime.now(), null, null, 1, null, LocalDateTime.now(), null);
  }
}
