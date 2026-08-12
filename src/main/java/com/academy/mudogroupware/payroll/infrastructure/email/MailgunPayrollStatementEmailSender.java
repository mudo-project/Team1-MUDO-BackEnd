package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementEmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Profile("mailgun")
@RequiredArgsConstructor
public class MailgunPayrollStatementEmailSender implements PayrollStatementEmailSender {
  private final JavaMailSender mailSender;
  private final PayrollEmailProperties properties;

  @Override
  public SendResult send(String recipientEmail, String subject, String body, String attachmentName,
      byte[] attachment, String deliveryToken) {
    MimeMessage message = mailSender.createMimeMessage();
    try {
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(properties.from());
      helper.setTo(recipientEmail);
      helper.setSubject(subject);
      helper.setText(body, false);
      helper.addAttachment(attachmentName, new ByteArrayResource(attachment), "application/pdf");
      message.addHeader("X-Mailgun-Variables",
          "{\"deliveryToken\":\"" + deliveryToken + "\"}");
    } catch (MessagingException e) {
      throw new IllegalStateException("급여명세서 이메일을 구성할 수 없습니다.", e);
    }
    mailSender.send(message);
    return SendResult.success();
  }
}
