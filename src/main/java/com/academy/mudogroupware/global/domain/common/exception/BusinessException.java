package com.academy.mudogroupware.global.domain.common.exception;

import java.util.Map;

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

  protected BusinessException(ErrorCode e, String m, Map<String, Object> context) {
    super(e, m, context);
  }

  protected BusinessException(
      ErrorCode e, String m, Throwable cause, Map<String, Object> context) {
    super(e, m, cause, context);
  }
}
