package com.academy.mudogroupware.global.domain.common.exception;

public class UnauthorizedException extends BusinessException {
  public UnauthorizedException() {
    super(CommonErrorCode.UNAUTHORIZED);
  }

  public UnauthorizedException(String m) {
    super(CommonErrorCode.UNAUTHORIZED, m);
  }

  protected UnauthorizedException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  protected UnauthorizedException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }
}
