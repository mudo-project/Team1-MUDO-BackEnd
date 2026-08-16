package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailStatusPort;
import java.time.LocalDateTime;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!mailgun")
public class DisabledPayrollStatementEmailStatusAdapter implements PayrollStatementEmailStatusPort {
  @Override
  public ProviderDeliveryStatus find(
      String messageId, String deliveryToken, LocalDateTime requestedAt) {
    return ProviderDeliveryStatus.notFound();
  }
}
