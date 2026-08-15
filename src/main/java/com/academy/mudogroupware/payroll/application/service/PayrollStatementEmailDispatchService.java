package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementDeliveryPort;
import java.time.LocalDateTime;
import java.util.List;
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

  public DispatchResult dispatch() {
    int batchSize = policy.dispatchBatchSize();
    List<Long> ids;
    try {
      ids = deliveries.findDispatchableIds(LocalDateTime.now(), batchSize);
    } catch (RuntimeException e) {
      log.warn("event=payroll_statement_email_dispatch_실패 errorType={}",
          e.getClass().getSimpleName());
      throw e;
    }
    if (ids.isEmpty()) {
      log.debug("event=payroll_statement_email_dispatch_시작 batchSize={}", batchSize);
      log.debug(
          "event=payroll_statement_email_dispatch_완료 processedCount=0, failureCount=0");
      return new DispatchResult(0, 0);
    }

    log.info("event=payroll_statement_email_dispatch_시작 batchSize={}, targetCount={}",
        batchSize, ids.size());
    int failures = 0;
    for (Long id : ids) {
      try {
        processor.processPending(id);
      } catch (RuntimeException e) {
        failures++;
        log.warn("event=payroll_statement_email_dispatch_실패 deliveryId={}, errorType={}",
            id, e.getClass().getSimpleName());
      }
    }
    DispatchResult result = new DispatchResult(ids.size(), failures);
    log.info(
        "event=payroll_statement_email_dispatch_완료 processedCount={}, failureCount={}",
        result.processedCount(), result.failureCount());
    return result;
  }

  public record DispatchResult(int processedCount, int failureCount) {}
}
