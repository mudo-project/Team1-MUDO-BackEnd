package com.academy.mudogroupware.payroll.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PayrollErrorCode implements ErrorCode {
  PAYROLL_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYROLL_NOT_FOUND", "급여를 찾을 수 없습니다."),
  PAYROLL_ALREADY_EXISTS(HttpStatus.CONFLICT, "PAYROLL_ALREADY_EXISTS", "해당 직원의 급여가 이미 존재합니다."),
  INVALID_PAYROLL_STATE(HttpStatus.CONFLICT, "INVALID_PAYROLL_STATE", "현재 급여 상태에서는 요청을 처리할 수 없습니다."),
  PAYROLL_VERSION_CONFLICT(HttpStatus.CONFLICT, "PAYROLL_VERSION_CONFLICT", "급여가 다른 요청에 의해 변경되었습니다."),
  PAYROLL_POLICY_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "PAYROLL_POLICY_NOT_FOUND", "급여 정책이 없습니다."),
  COMPENSATION_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "COMPENSATION_NOT_FOUND", "적용할 급여 계약이 없습니다."),
  COMPENSATION_PERIOD_OVERLAP(HttpStatus.CONFLICT, "COMPENSATION_PERIOD_OVERLAP", "급여 계약 기간이 겹칩니다."),
  PAYROLL_REFERENCE_DATA_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "PAYROLL_REFERENCE_DATA_MISSING", "급여 계산 기준 데이터가 부족합니다."),
  PAYROLL_ITEM_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "PAYROLL_ITEM_NOT_EDITABLE", "수정할 수 없는 급여 항목입니다."),
  PAYROLL_STATEMENT_NOT_READY(HttpStatus.CONFLICT, "PAYROLL_STATEMENT_NOT_READY", "급여명세서가 아직 준비되지 않았습니다."),
  PAYROLL_STATEMENT_RETRY_NOT_ALLOWED(HttpStatus.CONFLICT, "PAYROLL_STATEMENT_RETRY_NOT_ALLOWED", "실패한 급여명세서만 재시도할 수 있습니다."),
  INVALID_PAYROLL_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_PAYROLL_REQUEST", "급여 요청 값이 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
