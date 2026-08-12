package com.academy.mudogroupware.payroll.presentation.api;

import static com.academy.mudogroupware.payroll.presentation.api.common.PayrollResponseCode.STATEMENT_EMAIL_WEBHOOK_RECEIVED;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.payroll.application.service.PayrollEmailWebhookService;
import com.academy.mudogroupware.payroll.application.service.PayrollEmailWebhookService.WebhookCommand;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PayrollEmailWebhookController {
  private final PayrollEmailWebhookService service;

  @PostMapping("/api/webhooks/mailgun")
  @SecurityRequirements
  @Operation(summary = "Mailgun 급여명세서 이메일 발송 상태 수신")
  public GlobalApiResponse<Void> receive(@RequestBody JsonNode body) {
    JsonNode signature = body.path("signature");
    JsonNode eventData = body.path("event-data");
    JsonNode deliveryStatus = eventData.path("delivery-status");
    JsonNode variables = eventData.path("user-variables");
    service.handle(new WebhookCommand(
        text(signature, "timestamp"), text(signature, "token"), text(signature, "signature"),
        text(eventData, "event"), text(eventData, "severity"),
        eventData.path("timestamp").asDouble(0), text(variables, "deliveryToken"),
        text(eventData.path("message").path("headers"), "message-id"),
        text(deliveryStatus, "message")));
    return GlobalApiResponse.ok(STATEMENT_EMAIL_WEBHOOK_RECEIVED);
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }
}
