package com.academy.mudogroupware.attendance.presentation.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.usecase.GetMyAttendanceDashboardUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyMonthlyAttendanceUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetMyTodayAttendanceUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.response.MyAttendanceDashboardResponse;
import com.academy.mudogroupware.attendance.presentation.api.response.MyMonthlyAttendanceResponse;
import com.academy.mudogroupware.attendance.presentation.api.response.MyTodayAttendanceResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@Validated
@RestController
@RequestMapping("/api/attendance/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyAttendanceController {

    private final GetMyMonthlyAttendanceUseCase monthlyAttendanceUseCase;
    private final GetMyTodayAttendanceUseCase todayAttendanceUseCase;
    private final GetMyAttendanceDashboardUseCase dashboardUseCase;

    @Operation(summary = "내 월별 근태 조회",
            description = "입사일부터 오늘까지의 지정 월 근태를 캘린더 형식으로 조회합니다.")
    @GetMapping("/monthly")
    public GlobalApiResponse<MyMonthlyAttendanceResponse> getMonthly(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(example = "2026") @RequestParam @Min(1) @Max(9999) int year,
            @Parameter(example = "8") @RequestParam @Min(1) @Max(12) int month) {
        return GlobalApiResponse.ok(
                AttendanceResponseCode.MY_MONTHLY_ATTENDANCE_RETRIEVED,
                MyMonthlyAttendanceResponse.from(monthlyAttendanceUseCase.getMonthly(
                        authUser.userId(), authUser.academyId(), year, month)));
    }

    @Operation(summary = "내 오늘 근태 조회",
            description = "오늘 근무 예정 시간, 출퇴근 시각, 상태와 서버 기준 시각을 조회합니다.")
    @GetMapping("/today")
    public GlobalApiResponse<MyTodayAttendanceResponse> getToday(
            @AuthenticationPrincipal AuthUser authUser) {
        return GlobalApiResponse.ok(
                AttendanceResponseCode.MY_TODAY_ATTENDANCE_RETRIEVED,
                MyTodayAttendanceResponse.from(todayAttendanceUseCase.getToday(
                        authUser.userId(), authUser.academyId())));
    }

    @Operation(summary = "내 근태 대시보드 조회",
            description = "월별 근태, 오늘 근태, 연가 및 재직 조회 결과를 한 번에 조합합니다.")
    @GetMapping("/dashboard")
    public GlobalApiResponse<MyAttendanceDashboardResponse> getDashboard(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(example = "2026") @RequestParam @Min(1) @Max(9999) int year,
            @Parameter(example = "8") @RequestParam @Min(1) @Max(12) int month) {
        return GlobalApiResponse.ok(
                AttendanceResponseCode.MY_ATTENDANCE_DASHBOARD_RETRIEVED,
                MyAttendanceDashboardResponse.from(dashboardUseCase.getDashboard(
                        authUser.userId(), authUser.academyId(), year, month)));
    }
}
