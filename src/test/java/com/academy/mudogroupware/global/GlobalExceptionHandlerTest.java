package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.domain.common.exception.CommonErrorCode;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiErrorResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Test
  void bindExceptionReturnsFieldDetailsAndTraceId() {
    MDC.put("traceId", "trace123");
    BindException exception = new BindException(new Object(), "request");
    exception.addError(new FieldError("request", "name", "이름은 필수입니다."));

    ResponseEntity<GlobalApiErrorResponse> response = handler.binding(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.INVALID_INPUT.getCode());
    assertThat(response.getBody().traceId()).isEqualTo("trace123");
    assertThat(response.getBody().details()).containsKey("errors");
  }
}
