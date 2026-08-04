package com.academy.mudogroupware.global.domain.common.exception;

public class BadRequestException extends BusinessException {
  public BadRequestException() {
    super(CommonErrorCode.INVALID_INPUT);
  }

  public BadRequestException(String m) {
    super(CommonErrorCode.INVALID_INPUT, m);
  }

  protected BadRequestException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected BadRequestException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
