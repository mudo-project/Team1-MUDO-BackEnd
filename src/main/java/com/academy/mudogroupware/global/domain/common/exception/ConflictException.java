package com.academy.mudogroupware.global.domain.common.exception;

public class ConflictException extends BusinessException {
  public ConflictException() {
    super(CommonErrorCode.CONFLICT);
  }

  public ConflictException(String m) {
    super(CommonErrorCode.CONFLICT, m);
  }
}
