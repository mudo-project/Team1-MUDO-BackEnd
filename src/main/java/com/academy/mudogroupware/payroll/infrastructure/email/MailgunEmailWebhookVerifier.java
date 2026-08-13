package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.port.out.PayrollEmailWebhookVerifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MailgunEmailWebhookVerifier implements PayrollEmailWebhookVerifier {
  private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(5);
  private final PayrollEmailProperties properties;
  private final Clock clock = Clock.systemUTC();

  @Override
  public boolean verify(String timestamp, String token, String signature) {
    if (blank(timestamp) || blank(token) || blank(signature)
        || blank(properties.webhookSigningKey())) return false;
    try {
      Instant sentAt = Instant.ofEpochSecond(Long.parseLong(timestamp));
      if (Duration.between(sentAt, clock.instant()).abs().compareTo(ALLOWED_CLOCK_SKEW) > 0) {
        return false;
      }
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(properties.webhookSigningKey().getBytes(StandardCharsets.UTF_8),
          "HmacSHA256"));
      byte[] expected = mac.doFinal((timestamp + token).getBytes(StandardCharsets.UTF_8));
      byte[] actual = java.util.HexFormat.of().parseHex(signature);
      return MessageDigest.isEqual(expected, actual);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
