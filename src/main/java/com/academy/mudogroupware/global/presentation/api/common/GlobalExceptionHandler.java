package com.academy.mudogroupware.global.presentation.api.common;

import com.academy.mudogroupware.global.domain.common.exception.*;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<GlobalApiErrorResponse> application(ApplicationException e) {
    log.warn(
        "event=exception_handled errorCode={} traceId={}", e.getErrorCode().getCode(), trace());
    return ResponseEntity.status(e.getErrorCode().getHttpStatus())
        .body(GlobalApiErrorResponse.of(e, trace()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<GlobalApiErrorResponse> validation(MethodArgumentNotValidException e) {
    List<Map<String, Object>> errors =
        e.getBindingResult().getFieldErrors().stream().map(this::field).toList();
    return ResponseEntity.badRequest()
        .body(
            GlobalApiErrorResponse.of(
                CommonErrorCode.INVALID_INPUT, trace(), Map.of("errors", errors)));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<GlobalApiErrorResponse> unreadable(HttpMessageNotReadableException e) {
    return ResponseEntity.badRequest()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INVALID_INPUT, trace()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<GlobalApiErrorResponse> conflict(DataIntegrityViolationException e) {
    return ResponseEntity.status(CommonErrorCode.CONFLICT.getHttpStatus())
        .body(GlobalApiErrorResponse.of(CommonErrorCode.CONFLICT, trace()));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<GlobalApiErrorResponse> denied(AuthorizationDeniedException e) {
    return ResponseEntity.status(CommonErrorCode.ACCESS_DENIED.getHttpStatus())
        .body(GlobalApiErrorResponse.of(CommonErrorCode.ACCESS_DENIED, trace()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<GlobalApiErrorResponse> unexpected(Exception e) {
    log.error("event=exception_handled reason=unexpected traceId={}", trace(), e);
    return ResponseEntity.internalServerError()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR, trace()));
  }

  private Map<String, Object> field(FieldError e) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("field", e.getField());
    m.put("reason", e.getDefaultMessage());
    return m;
  }

  private String trace() {
    return MDC.get("traceId");
  }
}
