package com.academy.mudogroupware.payroll.application.port.out;

public interface PayrollStatementEmailSender {
  SendResult send(String recipientEmail, String subject, String body, String attachmentName,
      byte[] attachment, String deliveryToken);

  record SendResult(boolean sent, String skipCode) {
    public static SendResult success() {
      return new SendResult(true, null);
    }

    public static SendResult skipped(String code) {
      return new SendResult(false, code);
    }
  }
}
