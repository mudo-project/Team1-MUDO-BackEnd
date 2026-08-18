package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollStatementEmailRecoveryService {
  private final PayrollStatementDeliveryPort deliveries;
  private final Clock clock;

  @Transactional
  public int recover(Duration timeout) {
    LocalDateTime now = LocalDateTime.now(clock);
    return deliveries.markStaleSendingUnknown(now.minus(timeout), now);
  }
}
