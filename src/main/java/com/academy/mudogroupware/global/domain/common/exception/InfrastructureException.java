package com.academy.mudogroupware.global.domain.common.exception;

public abstract class InfrastructureException extends ApplicationException {
  protected InfrastructureException(ErrorCode e) {
    super(e);
  }

  protected InfrastructureException(ErrorCode e, String m) {
    super(e, m);
  }

  protected InfrastructureException(ErrorCode e, String m, Throwable c) {
    super(e, m, c);
  }
}
