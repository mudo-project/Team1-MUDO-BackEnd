package com.academy.mudogroupware.revenuereport.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class RevenueReportAiException extends BusinessException {
    public RevenueReportAiException(String message) {
        super(RevenueReportErrorCode.REVENUE_REPORT_AI_ERROR, message);
    }

    public RevenueReportAiException(String message, Throwable cause) {
        super(RevenueReportErrorCode.REVENUE_REPORT_AI_ERROR, message, cause);
    }
}
