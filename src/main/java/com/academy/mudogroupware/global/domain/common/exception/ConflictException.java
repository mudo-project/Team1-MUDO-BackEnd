package com.academy.mudogroupware.global.domain.common.exception;

public class ConflictException extends BusinessException {
  public ConflictException() {
    super(CommonErrorCode.CONFLICT);
  }

  public ConflictException(String m) {
    super(CommonErrorCode.CONFLICT, m);
  }

  protected ConflictException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected ConflictException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
