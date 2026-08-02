package com.academy.mudogroupware.global.domain.common.exception;

public class ForbiddenException extends BusinessException {
  public ForbiddenException() {
    super(CommonErrorCode.ACCESS_DENIED);
  }

  public ForbiddenException(String m) {
    super(CommonErrorCode.ACCESS_DENIED, m);
  }
}
