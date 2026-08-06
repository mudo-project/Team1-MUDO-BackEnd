package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.attendance.application.query.TodayTeamAttendanceView;
import com.academy.mudogroupware.attendance.domain.model.TeamAttendanceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

public record TodayTeamAttendanceResponse(
        @Schema(description = "조회 날짜", example = "2026-08-05")
        LocalDate date,
        @Schema(description = "요일", example = "수")
        String dayOfWeek,
        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "오늘 적용되는 정규 출근 시간", example = "09:00")
        LocalTime regularWorkStartTime,
        @JsonFormat(pattern = "HH:mm")
        @Schema(description = "오늘 적용되는 정규 퇴근 시간", example = "18:00")
        LocalTime regularWorkEndTime,
        Summary summary,
        List<Employee> employees
) {
    public static TodayTeamAttendanceResponse from(TodayTeamAttendanceView view) {
        return new TodayTeamAttendanceResponse(
                view.date(),
                view.dayOfWeek(),
                view.regularWorkStartTime(),
                view.regularWorkEndTime(),
                new Summary(
                        view.summary().presentCount(),
                        view.summary().absentCount(),
                        view.summary().offCount()),
                view.employees().stream().map(Employee::from).toList());
    }

    public record Summary(
            @Schema(description = "출근 인원", example = "6")
            int presentCount,
            @Schema(description = "미출근 인원", example = "1")
            int absentCount,
            @Schema(description = "휴무 인원", example = "0")
            int offCount
    ) {
    }

    public record Employee(
            @Schema(description = "사용자 ID", example = "2")
            Long userId,
            @Schema(description = "직원 이름", example = "김지수")
            String name,
            @Schema(description = "오늘 출결 상태", example = "PRESENT")
            TeamAttendanceStatus status,
            @JsonFormat(pattern = "HH:mm")
            @Schema(description = "출근 시각. 출근하지 않았거나 휴무일이면 null", example = "08:52")
            LocalTime checkInTime
    ) {
        private static Employee from(TodayTeamAttendanceView.Employee employee) {
            return new Employee(
                    employee.userId(), employee.name(), employee.status(), employee.checkInTime());
        }
    }
}
