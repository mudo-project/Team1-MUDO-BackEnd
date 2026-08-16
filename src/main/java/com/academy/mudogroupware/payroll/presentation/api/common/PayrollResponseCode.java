package com.academy.mudogroupware.payroll.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PayrollResponseCode implements ResponseCode {
  LIST_RETRIEVED("PAYROLL_200_1", "급여 목록을 조회했습니다."),
  CREATED("PAYROLL_201_1", "급여 초안을 생성했습니다."),
  CALCULATED("PAYROLL_200_2", "급여를 계산했습니다."),
  RETRIEVED("PAYROLL_200_3", "급여를 조회했습니다."),
  UPDATED("PAYROLL_200_4", "급여를 수정했습니다."),
  EARNING_ADDED("PAYROLL_201_2", "지급항목을 추가했습니다."),
  EARNING_DELETED("PAYROLL_200_5", "지급항목을 삭제했습니다."),
  CONFIRMED("PAYROLL_200_6", "급여를 확정했습니다."),
  REVISION_CREATED("PAYROLL_201_3", "급여 정정본을 생성했습니다."),
  REVISIONS_RETRIEVED("PAYROLL_200_7", "급여 정정 이력을 조회했습니다."),
  PREVIEW_RETRIEVED("PAYROLL_200_8", "급여명세서 미리보기를 조회했습니다."),
  STATEMENT_URL_ISSUED("PAYROLL_200_9", "급여명세서 다운로드 URL을 발급했습니다."),
  STATEMENT_RETRY_STARTED("PAYROLL_200_10", "급여명세서 생성을 재시도합니다."),
  POLICY_RETRIEVED("PAYROLL_200_11", "급여 정책을 조회했습니다."),
  POLICY_UPDATED("PAYROLL_200_12", "급여 정책을 수정했습니다."),
  SETTINGS_RETRIEVED("PAYROLL_200_13", "직원 급여 설정을 조회했습니다."),
  SETTINGS_UPDATED("PAYROLL_200_14", "직원 급여 설정을 저장했습니다."),
  STATEMENT_EMAIL_SEND_STARTED("PAYROLL_201_4", "급여명세서 이메일 발송을 시작했습니다."),
  STATEMENT_EMAIL_BATCH_STARTED("PAYROLL_201_5", "급여명세서 이메일 일괄 발송을 시작했습니다."),
  STATEMENT_EMAIL_BATCH_RETRIEVED("PAYROLL_200_15", "급여명세서 이메일 일괄 발송 결과를 조회했습니다."),
  STATEMENT_EMAIL_WEBHOOK_RECEIVED("PAYROLL_200_16", "이메일 발송 상태를 반영했습니다."),
  STATEMENT_EMAIL_DELIVERY_REUSED("PAYROLL_200_17", "기존 급여명세서 이메일 발송 이력을 조회했습니다.");

  private final String code;
  private final String message;
}
