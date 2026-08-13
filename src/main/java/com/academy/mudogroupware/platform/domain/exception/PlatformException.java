package com.academy.mudogroupware.platform.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class PlatformException extends BusinessException {
  public PlatformException(PlatformErrorCode errorCode) {
    super(errorCode);
  }

  public PlatformException(PlatformErrorCode errorCode, Throwable cause) {
    super(errorCode, errorCode.getMessage(), cause);
  }
}
