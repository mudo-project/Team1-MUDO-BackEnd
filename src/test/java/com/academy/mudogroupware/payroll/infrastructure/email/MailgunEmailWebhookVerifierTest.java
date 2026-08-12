package com.academy.mudogroupware.payroll.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class MailgunEmailWebhookVerifierTest {
  private static final String SIGNING_KEY = "test-signing-key";
  private final MailgunEmailWebhookVerifier verifier = new MailgunEmailWebhookVerifier(
      new PayrollEmailProperties("no-reply@example.com", SIGNING_KEY, Duration.ofMinutes(15)));

  @Test
  void 현재_시각의_정상_HMAC_서명을_허용한다() throws Exception {
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String token = "webhook-token";
    assertThat(verifier.verify(timestamp, token, signature(timestamp, token))).isTrue();
  }

  @Test
  void 재전송_허용시간을_지난_서명은_거절한다() throws Exception {
    String timestamp = String.valueOf(Instant.now().minus(Duration.ofMinutes(6)).getEpochSecond());
    String token = "webhook-token";
    assertThat(verifier.verify(timestamp, token, signature(timestamp, token))).isFalse();
  }

  private String signature(String timestamp, String token) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SIGNING_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(
        mac.doFinal((timestamp + token).getBytes(StandardCharsets.UTF_8)));
  }
}
