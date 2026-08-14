package com.academy.mudogroupware.payroll.infrastructure.email;

import com.academy.mudogroupware.payroll.application.service.PayrollStatementEmailDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayrollStatementEmailDispatchScheduler {
  private final PayrollStatementEmailDispatchService service;

  @Scheduled(fixedDelayString = "${app.mail.dispatch-interval-ms:10000}")
  public void dispatch() {
    service.dispatch();
  }
}
