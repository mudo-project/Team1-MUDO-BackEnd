package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollStatementEmailRecoveryService {
  private final PayrollStatementDeliveryPort deliveries;

  @Transactional
  public int recover(Duration timeout) {
    LocalDateTime now = LocalDateTime.now();
    return deliveries.markStaleSendingUnknown(now.minus(timeout), now);
  }
}
