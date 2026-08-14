package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailStatusPort;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("mailgun")
public class MailgunPayrollStatementEmailStatusAdapter implements PayrollStatementEmailStatusPort {
  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final DateTimeFormatter RFC_2822 = DateTimeFormatter.RFC_1123_DATE_TIME;
  private final RestClient restClient;
  private final PayrollEmailProperties properties;

  @Autowired
  public MailgunPayrollStatementEmailStatusAdapter(
      RestClient.Builder builder, PayrollEmailProperties properties) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(properties.connectTimeout());
    factory.setReadTimeout(properties.readTimeout());
    this.restClient = builder.baseUrl(properties.apiBaseUrl()).requestFactory(factory)
        .defaultHeaders(headers -> headers.setBasicAuth("api", properties.apiKey())).build();
    this.properties = properties;
  }

  MailgunPayrollStatementEmailStatusAdapter(
      RestClient restClient, PayrollEmailProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  @Override
  public ProviderDeliveryStatus find(
      String messageId, String deliveryToken, LocalDateTime requestedAt) {
    properties.validateHttpSending();
    JsonNode response = restClient.post().uri("/v1/analytics/logs")
        .contentType(MediaType.APPLICATION_JSON)
        .body(request(deliveryToken, requestedAt))
        .retrieve().body(JsonNode.class);
    if (response == null || !response.path("items").isArray()) {
      return ProviderDeliveryStatus.notFound();
    }
    for (JsonNode item : response.path("items")) {
      if (!hasTag(item, deliveryToken)) {
        continue;
      }
      ProviderDeliveryStatus mapped = map(item);
      if (mapped != null) return mapped;
    }
    return ProviderDeliveryStatus.notFound();
  }

  private Map<String, Object> request(String deliveryToken, LocalDateTime requestedAt) {
    return Map.of(
        "start", RFC_2822.format(requestedAt.minusMinutes(5).atZone(SEOUL)),
        "end", RFC_2822.format(Instant.now().atZone(ZoneOffset.UTC)),
        "events", List.of("accepted", "delivered", "failed", "rejected"),
        "filter", Map.of("AND", List.of(
            filter("domain", "=", properties.domain()),
            filter("tag", "=", deliveryToken))),
        "pagination", Map.of("sort", "timestamp:desc", "limit", 20));
  }

  private Map<String, Object> filter(String attribute, String comparator, String value) {
    return Map.of("attribute", attribute, "comparator", comparator,
        "values", List.of(Map.of("label", value, "value", value)));
  }

  private boolean hasTag(JsonNode item, String deliveryToken) {
    for (JsonNode tag : item.path("tags")) {
      if (deliveryToken.equals(tag.asText())) return true;
    }
    return false;
  }

  private ProviderDeliveryStatus map(JsonNode item) {
    String event = item.path("event").asText();
    String severity = item.path("severity").asText(
        item.path("delivery-status").path("severity").asText());
    ProviderStatus status = switch (event) {
      case "delivered" -> ProviderStatus.DELIVERED;
      case "failed", "rejected" -> "temporary".equals(severity)
          ? ProviderStatus.TEMPORARY_FAILURE : ProviderStatus.PERMANENT_FAILURE;
      case "accepted" -> ProviderStatus.ACCEPTED;
      default -> null;
    };
    if (status == null) return null;
    String providerMessageId = text(item.path("message").path("headers"), "message-id");
    LocalDateTime occurredAt = occurredAt(item);
    String reason = text(item.path("delivery-status"), "message");
    return new ProviderDeliveryStatus(status, providerMessageId, occurredAt, reason);
  }

  private LocalDateTime occurredAt(JsonNode item) {
    String timestamp = item.path("@timestamp").asText();
    if (!timestamp.isBlank()) {
      try {
        return LocalDateTime.ofInstant(OffsetDateTime.parse(timestamp).toInstant(), SEOUL);
      } catch (java.time.format.DateTimeParseException ignored) {
        return null;
      }
    }
    double legacyTimestamp = item.path("timestamp").asDouble(0);
    return legacyTimestamp <= 0 ? null
        : LocalDateTime.ofInstant(Instant.ofEpochSecond((long) legacyTimestamp), SEOUL);
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }
}
