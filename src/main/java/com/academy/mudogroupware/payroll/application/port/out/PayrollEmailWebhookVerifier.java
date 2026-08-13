package com.academy.mudogroupware.payroll.application.port.out;

public interface PayrollEmailWebhookVerifier {
  boolean verify(String timestamp, String token, String signature);
}
