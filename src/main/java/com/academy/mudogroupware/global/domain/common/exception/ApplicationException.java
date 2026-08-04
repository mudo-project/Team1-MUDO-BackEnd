package com.academy.mudogroupware.global.domain.common.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public abstract class ApplicationException extends RuntimeException {
  private final ErrorCode errorCode;
  private final Map<String, Object> context = new HashMap<>();

  protected ApplicationException(ErrorCode e) {
    this(e, e.getMessage());
  }

  protected ApplicationException(ErrorCode e, String m) {
    super(m);
    errorCode = e;
  }

  protected ApplicationException(ErrorCode e, String m, Throwable c) {
    super(m, c);
    errorCode = e;
  }

  protected ApplicationException(ErrorCode e, String m, Map<String, Object> c) {
    super(m);
    errorCode = e;
    if (c != null) context.putAll(c);
  }

  protected ApplicationException(
      ErrorCode e, String m, Throwable cause, Map<String, Object> c) {
    super(m, cause);
    errorCode = e;
    if (c != null) context.putAll(c);
  }

  protected void addContext(String k, Object v) {
    context.put(k, v);
  }

  public Map<String, Object> getContext() {
    return Map.copyOf(context);
  }
}
