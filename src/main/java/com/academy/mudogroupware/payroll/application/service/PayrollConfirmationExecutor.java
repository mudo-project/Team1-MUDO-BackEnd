package com.academy.mudogroupware.payroll.application.service;

import com.academy.mudogroupware.payroll.application.event.PayrollConfirmedEvent;
import com.academy.mudogroupware.payroll.application.port.out.PayrollRepository;
import com.academy.mudogroupware.payroll.application.port.out.PayrollStatementPort;
import com.academy.mudogroupware.payroll.domain.exception.PayrollErrorCode;
import com.academy.mudogroupware.payroll.domain.exception.PayrollException;
import com.academy.mudogroupware.payroll.domain.model.Payroll;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class PayrollConfirmationExecutor {
  private final PayrollRepository payrollRepository;
  private final PayrollStatementPort statementPort;
  private final ApplicationEventPublisher events;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  Payroll confirm(Long payrollId, long expectedVersion) {
    Payroll payroll = payrollRepository.findById(payrollId)
        .orElseThrow(() -> new PayrollException(PayrollErrorCode.PAYROLL_NOT_FOUND));
    if (payroll.getVersion() != expectedVersion) {
      throw new PayrollException(PayrollErrorCode.PAYROLL_VERSION_CONFLICT);
    }
    payroll.confirm(LocalDateTime.now());
    Payroll saved = payrollRepository.save(payroll);
    statementPort.createPendingIfAbsent(saved.getId());
    events.publishEvent(new PayrollConfirmedEvent(saved.getId()));
    return saved;
  }
}
