package com.academy.mudogroupware.revenuereport.presentation.api.response;

import java.time.LocalDate;

import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;

import io.swagger.v3.oas.annotations.media.Schema;

public record RevenueReportDetailResponse(
        @Schema(description = "리포트 ID", example = "1") Long reportId,
        @Schema(description = "대상 월(1일 날짜)", example = "2026-08-01") LocalDate targetMonth,
        @Schema(description = "AI가 생성한 서술 텍스트") String report,
        @Schema(description = "집계 당시 숫자 스냅샷(JSON 원문, 프론트 차트 렌더링용)") String dataSnapshot
) {
    public static RevenueReportDetailResponse from(RevenueReport report) {
        return new RevenueReportDetailResponse(
                report.getId(), report.getTargetMonth(), report.getReport(), report.getDataSnapshot());
    }
}
