package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollStatementEmailDispatchService {
  private final PayrollStatementDeliveryPort deliveries;
  private final PayrollStatementEmailProcessor processor;
  private final PayrollStatementEmailPolicy policy;

  public int dispatch() {
    var ids = deliveries.findDispatchableIds(LocalDateTime.now(), policy.dispatchBatchSize());
    for (Long id : ids) {
      try {
        processor.processPending(id);
      } catch (RuntimeException e) {
        log.warn("event=payroll_statement_email_dispatch_실패 deliveryId={}, errorType={}",
            id, e.getClass().getSimpleName());
      }
    }
    return ids.size();
  }
}
