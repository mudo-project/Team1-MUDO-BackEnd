package com.academy.mudogroupware.global.domain.common.exception;

public class InvalidArgumentException extends BusinessException {
  public InvalidArgumentException() {
    super(CommonErrorCode.INVALID_ARGUMENT);
  }

  public InvalidArgumentException(String reason) {
    super(CommonErrorCode.INVALID_ARGUMENT);
    addContext("reason", reason);
  }
}
