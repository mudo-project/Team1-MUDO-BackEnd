package com.academy.mudogroupware.global.domain.common.exception;

public class NotFoundException extends BusinessException {
  public NotFoundException() {
    super(CommonErrorCode.NOT_FOUND);
  }

  public NotFoundException(String m) {
    super(CommonErrorCode.NOT_FOUND, m);
  }
}
