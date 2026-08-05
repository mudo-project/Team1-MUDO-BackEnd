package com.academy.mudogroupware.attendance.presentation.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.query.TodayTeamAttendanceView;
import com.academy.mudogroupware.attendance.application.usecase.GetTodayTeamAttendanceUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.response.TodayTeamAttendanceResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@RestController
@RequestMapping("/api/attendance/team")
@RequiredArgsConstructor
public class AttendanceTeamController {

    private final GetTodayTeamAttendanceUseCase getTodayTeamAttendanceUseCase;

    @Operation(
            summary = "오늘 팀 근태 현황 조회",
            description = "원장이 소속 학원의 활성 직원별 오늘 출근, 미출근 또는 휴무 상태를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "오늘 팀 근태 현황 조회 성공"),
            @ApiResponse(responseCode = "403", description = "학원 원장이 아님"),
            @ApiResponse(responseCode = "404", description = "근무 정책이 등록되지 않음")
    })
    @PreAuthorize("hasAuthority('ATTENDANCE:READ')")
    @GetMapping("/today")
    public GlobalApiResponse<TodayTeamAttendanceResponse> getToday(
            @AuthenticationPrincipal AuthUser authUser) {
        TodayTeamAttendanceView view = getTodayTeamAttendanceUseCase.getToday(
                authUser.userId(), authUser.academyId());
        return GlobalApiResponse.ok(
                AttendanceResponseCode.TODAY_TEAM_ATTENDANCE_RETRIEVED,
                TodayTeamAttendanceResponse.from(view));
    }
}
