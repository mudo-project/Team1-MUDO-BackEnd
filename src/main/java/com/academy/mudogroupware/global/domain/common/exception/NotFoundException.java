package com.academy.mudogroupware.global.domain.common.exception;

import java.util.Map;

public class NotFoundException extends BusinessException {
  public NotFoundException() {
    super(CommonErrorCode.NOT_FOUND);
  }

  public NotFoundException(String m) {
    super(CommonErrorCode.NOT_FOUND, m);
  }

  protected NotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }

  protected NotFoundException(ErrorCode errorCode, String message) {
    super(errorCode, message);
  }

  protected NotFoundException(ErrorCode errorCode, String message, Throwable cause) {
    super(errorCode, message, cause);
  }

  protected NotFoundException(
      ErrorCode errorCode,
      String message,
      Throwable cause,
      Map<String, Object> context) {
    super(errorCode, message, cause, context);
  }
}
