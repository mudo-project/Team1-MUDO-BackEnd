package com.academy.mudogroupware.attendance.presentation.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.usecase.GetMyEmploymentSummaryUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.response.MyEmploymentSummaryResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@RestController
@RequestMapping("/api/users/me/employment-summary")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyEmploymentSummaryController {

    private final GetMyEmploymentSummaryUseCase employmentSummaryUseCase;

    @Operation(summary = "내 재직 정보 조회",
            description = "입사일과 오늘 기준 근속일수를 조회합니다.")
    @GetMapping
    public GlobalApiResponse<MyEmploymentSummaryResponse> getSummary(
            @AuthenticationPrincipal AuthUser authUser) {
        return GlobalApiResponse.ok(
                AttendanceResponseCode.MY_EMPLOYMENT_SUMMARY_RETRIEVED,
                MyEmploymentSummaryResponse.from(employmentSummaryUseCase.getSummary(
                        authUser.userId())));
    }
}
