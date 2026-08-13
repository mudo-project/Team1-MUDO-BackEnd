package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!mailgun")
@Slf4j
public class ConsolePayrollStatementEmailSender implements PayrollStatementEmailSender {
  @Override
  public SendResult send(String recipientEmail, String subject, String body, String attachmentName,
      byte[] attachment, String deliveryToken) {
    log.info("event=payroll_statement_email_send_완료 mode=disabled");
    return SendResult.skipped("MAIL_DELIVERY_DISABLED");
  }
}
