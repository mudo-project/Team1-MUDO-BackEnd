package com.academy.mudogroupware.payroll.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ApplicationException;

public class PayrollException extends ApplicationException {
  public PayrollException(PayrollErrorCode errorCode) {
    super(errorCode);
  }

  public PayrollException(PayrollErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
