package com.academy.mudogroupware.payroll.application.port.out;

public interface PayrollStatementEmailSender {
  SendResult send(String recipientEmail, String subject, String body, String attachmentName,
      byte[] attachment, String deliveryToken);

  record SendResult(boolean sent, String skipCode, String providerMessageId) {
    public static SendResult success(String providerMessageId) {
      return new SendResult(true, null, providerMessageId);
    }

    public static SendResult skipped(String code) {
      return new SendResult(false, code, null);
    }
  }

  enum FailureType { RETRYABLE, PERMANENT, UNKNOWN }

  final class SendException extends RuntimeException {
    private final FailureType failureType;
    private final String failureCode;

    public SendException(FailureType failureType, String failureCode, Throwable cause) {
      super(failureCode, cause);
      this.failureType = failureType;
      this.failureCode = failureCode;
    }

    public FailureType failureType() {
      return failureType;
    }

    public String failureCode() {
      return failureCode;
    }
  }
}
