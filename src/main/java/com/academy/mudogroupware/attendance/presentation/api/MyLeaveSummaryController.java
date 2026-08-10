package com.academy.mudogroupware.attendance.presentation.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.usecase.GetMyLeaveSummaryUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.response.MyLeaveSummaryResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@RestController
@RequestMapping("/api/leaves/me/summary")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyLeaveSummaryController {

    private final GetMyLeaveSummaryUseCase leaveSummaryUseCase;

    @Operation(summary = "내 연가 현황 조회",
            description = "현재 연가 지급 기간의 총·사용·결재 대기·잔여 일수를 조회합니다.")
    @GetMapping
    public GlobalApiResponse<MyLeaveSummaryResponse> getSummary(
            @AuthenticationPrincipal AuthUser authUser) {
        return GlobalApiResponse.ok(
                AttendanceResponseCode.MY_LEAVE_SUMMARY_RETRIEVED,
                MyLeaveSummaryResponse.from(leaveSummaryUseCase.getSummary(
                        authUser.userId())));
    }
}
