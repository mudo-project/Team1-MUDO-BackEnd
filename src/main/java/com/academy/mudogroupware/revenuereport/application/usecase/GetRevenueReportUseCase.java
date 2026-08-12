package com.academy.mudogroupware.revenuereport.application.usecase;

import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;

public interface GetRevenueReportUseCase {
    RevenueReport getReport(Long reportId);
}
