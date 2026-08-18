package com.academy.mudogroupware.revenuereport.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class RevenueReportAlreadyExistsException extends BusinessException {
    public RevenueReportAlreadyExistsException() {
        super(RevenueReportErrorCode.REVENUE_REPORT_ALREADY_EXISTS);
    }
}
