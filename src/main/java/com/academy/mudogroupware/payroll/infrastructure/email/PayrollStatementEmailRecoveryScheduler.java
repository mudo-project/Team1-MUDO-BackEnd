package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailRecoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollStatementEmailRecoveryScheduler {
  private final PayrollStatementEmailRecoveryService service;
  private final PayrollEmailProperties properties;

  @Scheduled(fixedDelayString = "${app.mail.recovery-interval-ms:300000}")
  public void recover() {
    service.recover(properties.sendingTimeout());
  }
}
