package com.academy.mudogroupware.attendance.presentation.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.usecase.GetWeeklyEmployeeAttendanceUseCase;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.response.WeeklyEmployeeAttendanceResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@RestController
@RequestMapping("/api/attendance/employees")
@RequiredArgsConstructor
@Validated
public class AttendanceWeeklyEmployeeController {

    private final GetWeeklyEmployeeAttendanceUseCase useCase;

    @Operation(summary = "전 직원 주간 출결 조회")
    @GetMapping("/weekly")
    @PreAuthorize("hasAuthority('ATTENDANCE:READ')")
    public GlobalApiResponse<WeeklyEmployeeAttendanceResponse> getWeekly(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MyAttendanceDayStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return GlobalApiResponse.ok(AttendanceResponseCode.WEEKLY_EMPLOYEE_ATTENDANCE_RETRIEVED,
                WeeklyEmployeeAttendanceResponse.from(useCase.getWeekly(
                        user.userId(), user.academyId(), date, keyword, status, page, size)));
    }
}
