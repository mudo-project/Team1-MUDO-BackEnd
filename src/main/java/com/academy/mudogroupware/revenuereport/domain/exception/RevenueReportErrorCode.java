package com.academy.mudogroupware.revenuereport.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RevenueReportErrorCode implements ErrorCode {
    REVENUE_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REVENUE_REPORT_404_1", "리포트를 찾을 수 없습니다."),
    REVENUE_REPORT_AI_ERROR(HttpStatus.BAD_GATEWAY, "REVENUE_REPORT_502_1", "매출 리포트 생성 중 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
