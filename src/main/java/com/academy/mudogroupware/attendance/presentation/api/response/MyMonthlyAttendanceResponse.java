package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.attendance.application.query.MyMonthlyAttendanceView;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record MyMonthlyAttendanceResponse(
        @Schema(example = "2026") int year,
        @Schema(example = "8") int month,
        List<Day> days) {

    public static MyMonthlyAttendanceResponse from(MyMonthlyAttendanceView view) {
        return new MyMonthlyAttendanceResponse(
                view.year(), view.month(), view.days().stream().map(Day::from).toList());
    }

    public record Day(
            @Schema(example = "2026-08-05") LocalDate date,
            @Schema(example = "LATE") MyAttendanceDayStatus status,
            @Schema(type = "string", example = "09:05:12", nullable = true)
            LocalTime clockInAt,
            @Schema(type = "string", example = "18:02:31", nullable = true)
            LocalTime clockOutAt) {
        private static Day from(MyMonthlyAttendanceView.Day day) {
            return new Day(day.date(), day.status(), day.clockInAt(), day.clockOutAt());
        }
    }
}
