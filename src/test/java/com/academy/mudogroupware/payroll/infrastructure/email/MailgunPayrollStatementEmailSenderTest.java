package com.academy.mudogroupware.payroll.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender.FailureType;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender.SendException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MailgunPayrollStatementEmailSenderTest {

  @Test
  void HTTP_API로_발송하고_Mailgun_메시지_ID를_반환한다() {
    RestClient.Builder builder = clientBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var sender = new MailgunPayrollStatementEmailSender(builder.build(), properties());
    String basic = Base64.getEncoder().encodeToString(
        "api:test-api-key".getBytes(StandardCharsets.UTF_8));
    server.expect(requestTo("https://api.mailgun.net/v3/mg.market-app.org/messages"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Basic " + basic))
        .andExpect(content().string(Matchers.containsString("delivery-token")))
        .andRespond(withSuccess(
            "{\"id\":\"<message@mailgun>\",\"message\":\"Queued. Thank you.\"}",
            MediaType.APPLICATION_JSON));

    var result = sender.send("staff@example.com", "subject", "body", "statement.pdf",
        new byte[] {1, 2}, "delivery-token");

    assertThat(result.providerMessageId()).isEqualTo("<message@mailgun>");
    server.verify();
  }

  @Test
  void Mailgun_5xx는_접수_여부가_불명확하므로_UNKNOWN으로_분류한다() {
    RestClient.Builder builder = clientBuilder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    var sender = new MailgunPayrollStatementEmailSender(builder.build(), properties());
    server.expect(requestTo("https://api.mailgun.net/v3/mg.market-app.org/messages"))
        .andRespond(withServerError());

    assertThatThrownBy(() -> sender.send("staff@example.com", "subject", "body",
        "statement.pdf", new byte[] {1}, "delivery-token"))
        .isInstanceOfSatisfying(SendException.class,
            exception -> assertThat(exception.failureType()).isEqualTo(FailureType.UNKNOWN));
  }

  private PayrollEmailProperties properties() {
    return new PayrollEmailProperties("no-reply@mg.market-app.org", "signing-key",
        Duration.ofMinutes(15), "https://api.mailgun.net", "mg.market-app.org", "test-api-key",
        Duration.ofSeconds(5), Duration.ofSeconds(10));
  }

  private RestClient.Builder clientBuilder() {
    return RestClient.builder().baseUrl("https://api.mailgun.net")
        .defaultHeaders(headers -> headers.setBasicAuth("api", "test-api-key"));
  }
}
