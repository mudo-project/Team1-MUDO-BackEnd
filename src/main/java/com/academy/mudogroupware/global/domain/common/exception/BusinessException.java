package com.academy.mudogroupware.global.domain.common.exception;

public abstract class BusinessException extends ApplicationException {
  protected BusinessException(ErrorCode e) {
    super(e);
  }

  protected BusinessException(ErrorCode e, String m) {
    super(e, m);
  }

  protected BusinessException(ErrorCode e, String m, Throwable c) {
    super(e, m, c);
  }
}
