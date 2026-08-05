package com.academy.mudogroupware.global.infrastructure.executor;

import com.academy.mudogroupware.global.domain.common.exception.ApplicationException;
import com.academy.mudogroupware.global.domain.common.exception.CommonErrorCode;

public class HeavyJobLimitExceededException extends ApplicationException {
  public HeavyJobLimitExceededException() {
    super(CommonErrorCode.TOO_MANY_REQUESTS, "무거운 작업이 이미 실행 중입니다. 잠시 후 다시 시도해 주세요.");
  }
}
