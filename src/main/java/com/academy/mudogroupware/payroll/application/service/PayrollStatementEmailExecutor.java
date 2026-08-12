package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort.DeliveryData;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class PayrollStatementEmailExecutor {
  private final PayrollStatementDeliveryPort deliveries;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  Optional<DeliveryData> claim(Long deliveryId) {
    return deliveries.claim(deliveryId, LocalDateTime.now());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void sent(Long deliveryId) {
    deliveries.markSent(deliveryId, LocalDateTime.now());
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void skipped(Long deliveryId, String code) {
    deliveries.markSkipped(deliveryId, code);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void failed(Long deliveryId, String code, String reason) {
    deliveries.markFailed(deliveryId, code, reason, LocalDateTime.now());
  }
}
