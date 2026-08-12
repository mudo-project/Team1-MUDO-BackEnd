package com.academy.mudogroupware.revenuereport.application.port;

import com.academy.mudogroupware.revenuereport.application.service.RevenueSnapshot;

public interface RevenueReportAiPort {

    String generateReport(RevenueSnapshot snapshot);
}
