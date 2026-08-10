package com.academy.mudogroupware.attendance.presentation.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.usecase.GetWeeklyEmployeeDetailUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.response.WeeklyEmployeeDetailResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@RestController
@RequestMapping("/api/attendance/employees")
@RequiredArgsConstructor
public class AttendanceWeeklyEmployeeDetailController {
    private final GetWeeklyEmployeeDetailUseCase useCase;

    @Operation(summary = "특정 직원 주간 출결 상세 조회")
    @GetMapping("/{userId}/weekly")
    @PreAuthorize("hasAuthority('ATTENDANCE:READ')")
    public GlobalApiResponse<WeeklyEmployeeDetailResponse> getWeeklyDetail(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return GlobalApiResponse.ok(AttendanceResponseCode.WEEKLY_EMPLOYEE_DETAIL_RETRIEVED,
                WeeklyEmployeeDetailResponse.from(useCase.getWeeklyDetail(
                        user.userId(), userId, date)));
    }
}
