package com.academy.mudogroupware.revenuereport.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BusinessException;

public class RevenueReportNotFoundException extends BusinessException {
    public RevenueReportNotFoundException() {
        super(RevenueReportErrorCode.REVENUE_REPORT_NOT_FOUND);
    }
}
