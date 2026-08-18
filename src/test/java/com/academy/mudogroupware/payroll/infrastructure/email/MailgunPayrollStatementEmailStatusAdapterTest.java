package com.academy.mudogroupware.payroll.infrastructure.email;

import static com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailStatusPort.ProviderStatus.DELIVERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.LocalDateTime;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MailgunPayrollStatementEmailStatusAdapterTest {

  @Test
  void deliveryToken으로_유실된_delivered_이벤트를_조회한다() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://api.mailgun.net");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var adapter = new MailgunPayrollStatementEmailStatusAdapter(builder.build(), properties());
    server.expect(requestTo("https://api.mailgun.net/v1/analytics/logs"))
        .andExpect(content().string(Matchers.containsString("delivery-token")))
        .andExpect(content().string(Matchers.containsString("\"attribute\":\"tag\"")))
        .andRespond(withSuccess("""
            {"items":[{
              "event":"delivered",
              "@timestamp":"2026-08-14T09:00:00Z",
              "tags":["delivery-token"],
              "user-variables":"{\\\"deliveryToken\\\":\\\"delivery-token\\\"}",
              "message":{"headers":{"message-id":"message-id"}},
              "delivery-status":{"message":"delivered"}
            }]}
            """, MediaType.APPLICATION_JSON));

    var result = adapter.find(null, "delivery-token",
        LocalDateTime.of(2026, 8, 14, 9, 0));

    assertThat(result.status()).isEqualTo(DELIVERED);
    assertThat(result.messageId()).isEqualTo("message-id");
    server.verify();
  }

  private PayrollEmailProperties properties() {
    return new PayrollEmailProperties("no-reply@mg.market-app.org", "signing-key",
        Duration.ofMinutes(15), "https://api.mailgun.net", "mg.market-app.org", "test-api-key",
        Duration.ofSeconds(5), Duration.ofSeconds(10));
  }
}
