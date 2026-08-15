package com.academy.mudogroupware.payroll.application.port.out;

import java.time.LocalDateTime;

public interface PayrollStatementEmailStatusPort {
  ProviderDeliveryStatus find(String messageId, String deliveryToken, LocalDateTime requestedAt);

  enum ProviderStatus { ACCEPTED, DELIVERED, TEMPORARY_FAILURE, PERMANENT_FAILURE, NOT_FOUND }

  record ProviderDeliveryStatus(ProviderStatus status, String messageId,
      LocalDateTime occurredAt, String reason) {
    public static ProviderDeliveryStatus notFound() {
      return new ProviderDeliveryStatus(ProviderStatus.NOT_FOUND, null, null, null);
    }
  }
}
