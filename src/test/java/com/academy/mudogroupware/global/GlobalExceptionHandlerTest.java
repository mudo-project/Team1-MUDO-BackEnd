package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.domain.common.exception.CommonErrorCode;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiErrorResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalExceptionHandler;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;

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

  @Test
  void missingParameterReturns400WithFieldName() {
    MDC.put("traceId", "trace456");
    MissingServletRequestParameterException exception =
        new MissingServletRequestParameterException("format", "String");

    ResponseEntity<GlobalApiErrorResponse> response = handler.missingParameter(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.INVALID_INPUT.getCode());
    assertThat(response.getBody().traceId()).isEqualTo("trace456");
    assertThat(response.getBody().details()).containsKey("errors");
  }

  @Test
  void illegalArgumentReturns400WithExceptionMessage() {
    MDC.put("traceId", "trace789");
    IllegalArgumentException exception = new IllegalArgumentException("카드 사용내역을 찾을 수 없습니다.");

    ResponseEntity<GlobalApiErrorResponse> response = handler.illegalArgument(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.INVALID_ARGUMENT.getCode());
    assertThat(response.getBody().message()).isEqualTo("카드 사용내역을 찾을 수 없습니다.");
    assertThat(response.getBody().traceId()).isEqualTo("trace789");
  }

  @Test
  void illegalStateReturns409WithExceptionMessage() {
    MDC.put("traceId", "trace999");
    IllegalStateException exception = new IllegalStateException("아직 정산 상신되지 않은 사용내역입니다.");

    ResponseEntity<GlobalApiErrorResponse> response = handler.illegalState(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(409);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.CONFLICT.getCode());
    assertThat(response.getBody().message()).isEqualTo("아직 정산 상신되지 않은 사용내역입니다.");
    assertThat(response.getBody().traceId()).isEqualTo("trace999");
  }

  @Test
  void completionExceptionWrappingApplicationExceptionUnwrapsToOriginalStatusAndCode() {
    MDC.put("traceId", "trace-completion-1");
    PlatformException cause = new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE);
    CompletionException exception = new CompletionException(cause);

    ResponseEntity<GlobalApiErrorResponse> response = handler.completion(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(503);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(PlatformErrorCode.METRICS_UNAVAILABLE.getCode());
    assertThat(response.getBody().traceId()).isEqualTo("trace-completion-1");
  }

  @Test
  void completionExceptionWrappingUnknownCauseFallsBackToInternalServerError() {
    MDC.put("traceId", "trace-completion-2");
    CompletionException exception = new CompletionException(new RuntimeException("boom"));

    ResponseEntity<GlobalApiErrorResponse> response = handler.completion(exception);

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
    assertThat(response.getBody().traceId()).isEqualTo("trace-completion-2");
  }
}
