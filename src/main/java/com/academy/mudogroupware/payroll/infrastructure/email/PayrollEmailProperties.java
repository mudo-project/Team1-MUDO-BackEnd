package com.academy.mudogroupware.payroll.infrastructure.email;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record PayrollEmailProperties(String from, String webhookSigningKey,
    Duration sendingTimeout) {
  public PayrollEmailProperties(
      @Value("${app.mail.from:}") String from,
      @Value("${app.mail.webhook-signing-key:}") String webhookSigningKey,
      @Value("${app.mail.sending-timeout:PT15M}") Duration sendingTimeout) {
    this.from = from;
    this.webhookSigningKey = webhookSigningKey;
    this.sendingTimeout = sendingTimeout;
  }
}
