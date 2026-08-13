package com.academy.mudogroupware.revenuereport.presentation.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.revenuereport.application.usecase.CountUnreadRevenueReportsUseCase;
import com.academy.mudogroupware.revenuereport.application.usecase.GetRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.application.usecase.ListRevenueReportsUseCase;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.presentation.api.common.RevenueReportResponseCode;
import com.academy.mudogroupware.revenuereport.presentation.api.response.RevenueReportDetailResponse;
import com.academy.mudogroupware.revenuereport.presentation.api.response.RevenueReportListItemResponse;
import com.academy.mudogroupware.revenuereport.presentation.api.response.UnreadCountResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "매출 리포트", description = "AI가 생성한 월간 매출 리포트 조회 API. 학원 계정(ACADEMY:OWNER)만 접근 가능.")
@RestController
@RequestMapping("/api/revenue-reports")
@RequiredArgsConstructor
public class RevenueReportController {

    private final ListRevenueReportsUseCase listRevenueReportsUseCase;
    private final GetRevenueReportUseCase getRevenueReportUseCase;
    private final CountUnreadRevenueReportsUseCase countUnreadRevenueReportsUseCase;

    @Operation(summary = "매출 리포트 목록 조회", description = "대상 월 최신순으로 반환하며, 항목마다 읽음 여부를 포함한다.")
    @PreAuthorize("hasAuthority('ACADEMY:OWNER')")
    @GetMapping
    public GlobalApiResponse<List<RevenueReportListItemResponse>> list() {
        List<RevenueReport> reports = listRevenueReportsUseCase.listReports();
        List<RevenueReportListItemResponse> data = reports.stream()
                .map(RevenueReportListItemResponse::from)
                .toList();
        return GlobalApiResponse.ok(RevenueReportResponseCode.REVENUE_REPORT_LIST_FETCHED, data);
    }

    @Operation(summary = "매출 리포트 상세 조회",
            description = "조회 시 해당 리포트를 읽음 처리한다(이미 읽었으면 최초 읽은 시각을 유지).")
    @PreAuthorize("hasAuthority('ACADEMY:OWNER')")
    @GetMapping("/{reportId}")
    public GlobalApiResponse<RevenueReportDetailResponse> detail(@PathVariable Long reportId) {
        RevenueReport report = getRevenueReportUseCase.getReport(reportId);
        return GlobalApiResponse.ok(
                RevenueReportResponseCode.REVENUE_REPORT_DETAIL_FETCHED, RevenueReportDetailResponse.from(report));
    }

    @Operation(summary = "안읽은 매출 리포트 수 조회", description = "탭 배지 등 가벼운 표시용. 목록 전체를 조회하지 않는다.")
    @PreAuthorize("hasAuthority('ACADEMY:OWNER')")
    @GetMapping("/unread-count")
    public GlobalApiResponse<UnreadCountResponse> unreadCount() {
        long count = countUnreadRevenueReportsUseCase.countUnread();
        return GlobalApiResponse.ok(
                RevenueReportResponseCode.REVENUE_REPORT_UNREAD_COUNT_FETCHED, UnreadCountResponse.from(count));
    }
}
