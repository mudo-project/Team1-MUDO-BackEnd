package com.academy.mudogroupware.global.presentation.api.common;

import com.academy.mudogroupware.global.domain.common.exception.*;
import jakarta.validation.ConstraintViolationException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<GlobalApiErrorResponse> application(ApplicationException e) {
    String traceId = trace();
    log.warn(
        "event=exception_handled reason={} code={} message={} traceId={} details={}",
        e.getErrorCode().name(),
        e.getErrorCode().getCode(),
        e.getMessage(),
        traceId,
        e.getContext());
    return ResponseEntity.status(e.getErrorCode().getHttpStatus())
        .body(GlobalApiErrorResponse.of(e, traceId));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<GlobalApiErrorResponse> validation(MethodArgumentNotValidException e) {
    List<Map<String, Object>> errors =
        e.getBindingResult().getFieldErrors().stream().map(this::field).toList();
    Map<String, Object> details = Map.of("errors", errors);
    String traceId = trace();
    log.warn("event=exception_handled reason=validation_failed traceId={} details={}", traceId, details);
    return ResponseEntity.badRequest()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INVALID_INPUT, traceId, details));
  }

  @ExceptionHandler(BindException.class)
  public ResponseEntity<GlobalApiErrorResponse> binding(BindException e) {
    List<Map<String, Object>> errors =
        e.getBindingResult().getFieldErrors().stream().map(this::field).toList();
    Map<String, Object> details = Map.of("errors", errors);
    String traceId = trace();
    log.warn("event=exception_handled reason=binding_failed traceId={} details={}", traceId, details);
    return ResponseEntity.badRequest()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INVALID_INPUT, traceId, details));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<GlobalApiErrorResponse> constraintViolation(ConstraintViolationException e) {
    List<Map<String, Object>> errors =
        e.getConstraintViolations().stream()
            .map(
                violation -> {
                  Map<String, Object> error = new LinkedHashMap<>();
                  error.put("field", violation.getPropertyPath().toString());
                  error.put("reason", violation.getMessage());
                  return error;
                })
            .toList();
    Map<String, Object> details = Map.of("errors", errors);
    String traceId = trace();
    log.warn(
        "event=exception_handled reason=constraint_violation traceId={} details={}",
        traceId,
        details);
    return ResponseEntity.badRequest()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INVALID_INPUT, traceId, details));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<GlobalApiErrorResponse> typeMismatch(
      MethodArgumentTypeMismatchException e) {
    String traceId = trace();
    Map<String, Object> error = new LinkedHashMap<>();
    error.put("field", e.getName());
    error.put("rejectedValue", e.getValue() == null ? "null" : e.getValue().toString());
    error.put("reason", "올바르지 않은 값입니다.");
    Map<String, Object> details = Map.of("errors", List.of(error));
    log.warn(
        "event=exception_handled reason=type_mismatch param={} value={} traceId={}",
        e.getName(),
        e.getValue(),
        traceId);
    return ResponseEntity.badRequest()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INVALID_INPUT, traceId, details));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<GlobalApiErrorResponse> missingParameter(MissingServletRequestParameterException e) {
    String traceId = trace();
    Map<String, Object> error = new LinkedHashMap<>();
    error.put("field", e.getParameterName());
    error.put("reason", "필수 파라미터입니다.");
    Map<String, Object> details = Map.of("errors", List.of(error));
    log.warn(
        "event=exception_handled reason=missing_parameter param={} traceId={}",
        e.getParameterName(),
        traceId);
    return ResponseEntity.badRequest()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INVALID_INPUT, traceId, details));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<GlobalApiErrorResponse> unreadable(HttpMessageNotReadableException e) {
    String traceId = trace();
    log.warn(
        "event=exception_handled reason=message_not_readable exceptionClass={} traceId={}",
        e.getClass().getSimpleName(),
        traceId);
    return ResponseEntity.badRequest()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INVALID_INPUT, traceId));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<GlobalApiErrorResponse> conflict(DataIntegrityViolationException e) {
    String traceId = trace();
    log.warn(
        "event=exception_handled reason=data_integrity_violation exceptionClass={} errorCode={} httpStatus={} traceId={}",
        e.getClass().getSimpleName(),
        CommonErrorCode.CONFLICT.getCode(),
        CommonErrorCode.CONFLICT.getHttpStatus().value(),
        traceId);
    return ResponseEntity.status(CommonErrorCode.CONFLICT.getHttpStatus())
        .body(GlobalApiErrorResponse.of(CommonErrorCode.CONFLICT, traceId));
  }

  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<GlobalApiErrorResponse> denied(AuthorizationDeniedException e) {
    String traceId = trace();
    log.warn(
        "event=exception_handled reason=authorization_denied exceptionClass={} errorCode={} httpStatus={} traceId={} message={}",
        e.getClass().getSimpleName(),
        CommonErrorCode.ACCESS_DENIED.getCode(),
        CommonErrorCode.ACCESS_DENIED.getHttpStatus().value(),
        traceId,
        e.getMessage());
    return ResponseEntity.status(CommonErrorCode.ACCESS_DENIED.getHttpStatus())
        .body(GlobalApiErrorResponse.of(CommonErrorCode.ACCESS_DENIED, traceId));
  }

  @ExceptionHandler(TaskRejectedException.class)
  public ResponseEntity<GlobalApiErrorResponse> taskRejected(TaskRejectedException e) {
    log.warn("event=async_task_rejected traceId={}", trace());
    return ResponseEntity.status(CommonErrorCode.SERVICE_UNAVAILABLE.getHttpStatus())
        .body(GlobalApiErrorResponse.of(CommonErrorCode.SERVICE_UNAVAILABLE, trace()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<GlobalApiErrorResponse> unexpected(Exception e) {
    String traceId = trace();
    log.error(
        "event=exception_handled reason=unexpected_exception exceptionClass={} errorCode={} httpStatus={} traceId={}",
        e.getClass().getSimpleName(),
        CommonErrorCode.INTERNAL_SERVER_ERROR.getCode(),
        CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value(),
        traceId,
        e);
    return ResponseEntity.internalServerError()
        .body(GlobalApiErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR, traceId));
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
