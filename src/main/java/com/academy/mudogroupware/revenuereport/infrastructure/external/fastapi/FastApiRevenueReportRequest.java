package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import com.academy.mudogroupware.revenuereport.application.service.RevenueSnapshot;

record FastApiRevenueReportRequest(RevenueSnapshot snapshot) {
    static FastApiRevenueReportRequest from(RevenueSnapshot snapshot) {
        return new FastApiRevenueReportRequest(snapshot);
    }
}
