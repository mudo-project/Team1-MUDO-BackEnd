package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.event.PayrollStatementEmailWorkChangedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollEmailWebhookVerifier;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.domain.exception.PayrollErrorCode;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollEmailWebhookService {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private final PayrollEmailWebhookVerifier verifier;
  private final PayrollStatementDeliveryPort deliveries;
  private final ApplicationEventPublisher events;

  @Transactional
  public void handle(WebhookCommand command) {
    if (!verifier.verify(command.signatureTimestamp(), command.signatureToken(),
        command.signature())) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_EMAIL_WEBHOOK_SIGNATURE_INVALID);
    }
    if (command.deliveryToken() == null
        || deliveries.findByToken(command.deliveryToken()).isEmpty()) return;
    long epochSecond = command.eventTimestamp() > 0 ? (long) command.eventTimestamp()
        : Long.parseLong(command.signatureTimestamp());
    LocalDateTime occurredAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), SEOUL);
    if ("delivered".equals(command.event())) {
      deliveries.markDelivered(command.deliveryToken(), command.messageId(), occurredAt);
      events.publishEvent(new PayrollStatementEmailWorkChangedEvent());
      return;
    }
    boolean permanent = "permanent_fail".equals(command.event())
        || ("failed".equals(command.event()) && "permanent".equals(command.severity()));
    if (permanent) {
      deliveries.markPermanentFailure(command.deliveryToken(), command.messageId(),
          command.reason(), occurredAt);
      events.publishEvent(new PayrollStatementEmailWorkChangedEvent());
    }
  }

  public record WebhookCommand(String signatureTimestamp, String signatureToken, String signature,
      String event, String severity, double eventTimestamp, String deliveryToken,
      String messageId, String reason) {}
}
