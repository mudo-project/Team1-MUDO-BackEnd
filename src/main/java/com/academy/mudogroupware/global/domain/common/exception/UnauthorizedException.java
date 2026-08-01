package com.academy.mudogroupware.global.domain.common.exception;

public class UnauthorizedException extends BusinessException {
  public UnauthorizedException() {
    super(CommonErrorCode.UNAUTHORIZED);
  }

  public UnauthorizedException(String m) {
    super(CommonErrorCode.UNAUTHORIZED, m);
  }
}
