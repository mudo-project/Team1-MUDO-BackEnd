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
  PAYROLL_REVISION_CONFLICT(HttpStatus.CONFLICT, "PAYROLL_REVISION_CONFLICT", "최신 급여에서만 정정본을 생성할 수 있습니다."),
  INVALID_PAYROLL_STATE(HttpStatus.CONFLICT, "INVALID_PAYROLL_STATE", "현재 급여 상태에서는 요청을 처리할 수 없습니다."),
  PAYROLL_VERSION_CONFLICT(HttpStatus.CONFLICT, "PAYROLL_VERSION_CONFLICT", "급여가 다른 요청에 의해 변경되었습니다."),
  PAYROLL_POLICY_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "PAYROLL_POLICY_NOT_FOUND", "급여 정책이 없습니다."),
  COMPENSATION_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "COMPENSATION_NOT_FOUND", "적용할 급여 계약이 없습니다."),
  COMPENSATION_PERIOD_OVERLAP(HttpStatus.CONFLICT, "COMPENSATION_PERIOD_OVERLAP", "급여 계약 기간이 겹칩니다."),
  PAYROLL_REFERENCE_DATA_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "PAYROLL_REFERENCE_DATA_MISSING", "급여 계산 기준 데이터가 부족합니다."),
  PAYROLL_ITEM_NOT_EDITABLE(HttpStatus.BAD_REQUEST, "PAYROLL_ITEM_NOT_EDITABLE", "수정할 수 없는 급여 항목입니다."),
  PAYROLL_STATEMENT_NOT_READY(HttpStatus.CONFLICT, "PAYROLL_STATEMENT_NOT_READY", "급여명세서가 아직 준비되지 않았습니다."),
  PAYROLL_STATEMENT_RETRY_NOT_ALLOWED(HttpStatus.CONFLICT, "PAYROLL_STATEMENT_RETRY_NOT_ALLOWED", "실패한 급여명세서만 재시도할 수 있습니다."),
  PAYROLL_EMPLOYEE_EMAIL_MISSING(HttpStatus.UNPROCESSABLE_ENTITY, "PAYROLL_EMAIL_422_1", "직원 이메일이 등록되어 있지 않습니다."),
  PAYROLL_EMAIL_NOT_LATEST_REVISION(HttpStatus.CONFLICT, "PAYROLL_EMAIL_409_1", "최신 급여 정정본만 이메일로 발송할 수 있습니다."),
  PAYROLL_EMAIL_DELIVERY_CONFLICT(HttpStatus.CONFLICT, "PAYROLL_EMAIL_409_2", "이미 전달됐거나 발송 처리 중인 급여명세서입니다."),
  PAYROLL_EMAIL_BATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYROLL_EMAIL_404_1", "이메일 일괄 발송 내역을 찾을 수 없습니다."),
  PAYROLL_EMAIL_WEBHOOK_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "PAYROLL_EMAIL_401_1", "Mailgun Webhook 서명이 올바르지 않습니다."),
  INVALID_PAYROLL_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_PAYROLL_REQUEST", "급여 요청 값이 올바르지 않습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
