package com.academy.mudogroupware.revenuereport.application.service;

import java.util.List;
import java.util.Map;

import com.academy.mudogroupware.revenuereport.application.port.ExpenseSummary;
import com.academy.mudogroupware.revenuereport.application.port.LectureRevenueInfo;
import com.academy.mudogroupware.revenuereport.domain.model.Payment;

record RevenueReportAggregation(
        List<LectureRevenueInfo> lectures,
        Map<Long, Long> activeEnrollmentCounts,
        List<Payment> payments,
        ExpenseSummary expenseSummary,
        Map<Long, Long> enrollmentIdToLectureId) {
}
