package com.academy.mudogroupware.revenuereport.presentation.api.response;

import java.time.LocalDate;

import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;

import io.swagger.v3.oas.annotations.media.Schema;

public record RevenueReportListItemResponse(
        @Schema(description = "리포트 ID", example = "1") Long reportId,
        @Schema(description = "대상 월(1일 날짜)", example = "2026-08-01") LocalDate targetMonth,
        @Schema(description = "읽음 여부", example = "false") boolean read
) {
    public static RevenueReportListItemResponse from(RevenueReport report) {
        return new RevenueReportListItemResponse(report.getId(), report.getTargetMonth(), report.isRead());
    }
}
