package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender.FailureType;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender.SendException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("mailgun")
public class MailgunPayrollStatementEmailSender implements PayrollStatementEmailSender {
  private final RestClient restClient;
  private final PayrollEmailProperties properties;

  @Autowired
  public MailgunPayrollStatementEmailSender(
      RestClient.Builder builder, PayrollEmailProperties properties) {
    properties.validateHttpSending();
    this.restClient = builder.baseUrl(properties.apiBaseUrl())
        .requestFactory(requestFactory(properties.connectTimeout(), properties.readTimeout()))
        .defaultHeaders(headers -> headers.setBasicAuth("api", properties.apiKey()))
        .build();
    this.properties = properties;
  }

  MailgunPayrollStatementEmailSender(RestClient restClient, PayrollEmailProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  public SendResult send(String recipientEmail, String subject, String body, String attachmentName,
      byte[] attachment, String deliveryToken) {
    properties.validateHttpSending();
    try {
      MailgunSendResponse response = restClient.post()
          .uri("/v3/{domain}/messages", properties.domain())
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(parts(recipientEmail, subject, body, attachmentName, attachment, deliveryToken))
          .retrieve()
          .body(MailgunSendResponse.class);
      if (response == null || response.id() == null || response.id().isBlank()) {
        throw new SendException(FailureType.UNKNOWN, "MAILGUN_RESPONSE_ID_MISSING", null);
      }
      return SendResult.success(response.id());
    } catch (HttpClientErrorException.TooManyRequests e) {
      throw new SendException(FailureType.RETRYABLE, "MAILGUN_RATE_LIMITED", e);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.REQUEST_TIMEOUT) {
        throw new SendException(FailureType.UNKNOWN, "MAILGUN_RESULT_UNKNOWN", e);
      }
      throw new SendException(FailureType.PERMANENT, "MAILGUN_REQUEST_REJECTED", e);
    } catch (HttpServerErrorException e) {
      throw new SendException(FailureType.UNKNOWN, "MAILGUN_SERVER_RESULT_UNKNOWN", e);
    } catch (ResourceAccessException e) {
      throw new SendException(FailureType.UNKNOWN, "MAILGUN_RESULT_UNKNOWN", e);
    } catch (SendException e) {
      throw e;
    } catch (RestClientException e) {
      throw new SendException(FailureType.UNKNOWN, "MAILGUN_RESULT_UNKNOWN", e);
    }
  }

  private MultiValueMap<String, Object> parts(String recipientEmail, String subject, String body,
      String attachmentName, byte[] attachment, String deliveryToken) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("from", properties.from());
    parts.add("to", recipientEmail);
    parts.add("subject", subject);
    parts.add("text", body);
    parts.add("v:deliveryToken", deliveryToken);
    parts.add("o:tag", deliveryToken);
    parts.add("attachment", new NamedByteArrayResource(attachment, attachmentName));
    return parts;
  }

  private static SimpleClientHttpRequestFactory requestFactory(
      Duration connectTimeout, Duration readTimeout) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    return factory;
  }

  record MailgunSendResponse(String id, String message) {}

  private static final class NamedByteArrayResource extends ByteArrayResource {
    private final String filename;

    private NamedByteArrayResource(byte[] bytes, String filename) {
      super(bytes);
      this.filename = filename;
    }

    @Override
    public String getFilename() {
      return filename;
    }
  }
}
