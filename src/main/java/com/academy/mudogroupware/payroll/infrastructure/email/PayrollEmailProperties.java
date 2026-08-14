package com.academy.mudogroupware.payroll.infrastructure.email;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record PayrollEmailProperties(String from, String webhookSigningKey,
    Duration sendingTimeout, String apiBaseUrl, String domain, String apiKey,
    Duration connectTimeout, Duration readTimeout) {
  @Autowired
  public PayrollEmailProperties(
      @Value("${app.mail.from:}") String from,
      @Value("${app.mail.webhook-signing-key:}") String webhookSigningKey,
      @Value("${app.mail.sending-timeout:PT15M}") Duration sendingTimeout,
      @Value("${app.mail.api-base-url:https://api.mailgun.net}") String apiBaseUrl,
      @Value("${app.mail.domain:mg.market-app.org}") String domain,
      @Value("${app.mail.api-key:}") String apiKey,
      @Value("${app.mail.connect-timeout:PT5S}") Duration connectTimeout,
      @Value("${app.mail.read-timeout:PT10S}") Duration readTimeout) {
    this.from = from;
    this.webhookSigningKey = webhookSigningKey;
    this.sendingTimeout = sendingTimeout;
    this.apiBaseUrl = apiBaseUrl;
    this.domain = domain;
    this.apiKey = apiKey;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
  }

  public PayrollEmailProperties(String from, String webhookSigningKey, Duration sendingTimeout) {
    this(from, webhookSigningKey, sendingTimeout, "https://api.mailgun.net",
        "mg.market-app.org", "", Duration.ofSeconds(5), Duration.ofSeconds(10));
  }

  void validateHttpSending() {
    if (blank(from) || blank(apiBaseUrl) || blank(domain) || blank(apiKey)) {
      throw new IllegalStateException("Mailgun HTTP API configuration is incomplete");
    }
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
