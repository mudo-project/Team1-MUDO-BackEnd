package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.domain.common.exception.*;
import org.junit.jupiter.api.Test;

class ApplicationExceptionTest {
  @Test
  void keepsErrorCode() {
    ApplicationException e = new BadRequestException("잘못된 값");
    assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT);
    assertThat(e.getMessage()).isEqualTo("잘못된 값");
  }
}
