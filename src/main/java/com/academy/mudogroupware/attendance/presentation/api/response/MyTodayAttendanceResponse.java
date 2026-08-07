package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import com.academy.mudogroupware.attendance.application.query.MyTodayAttendanceView;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record MyTodayAttendanceResponse(
        @Schema(example = "2026-08-07") LocalDate date,
        @Schema(type = "string", example = "09:00:00") LocalTime workStartTime,
        @Schema(type = "string", example = "18:00:00") LocalTime workEndTime,
        @Schema(example = "2026-08-07T09:05:12+09:00", nullable = true)
        OffsetDateTime clockInAt,
        @Schema(example = "2026-08-07T18:02:31+09:00", nullable = true)
        OffsetDateTime clockOutAt,
        @Schema(example = "LATE") MyAttendanceDayStatus status,
        @Schema(example = "2026-08-07T14:57:21+09:00") OffsetDateTime serverTime) {

    public static MyTodayAttendanceResponse from(MyTodayAttendanceView view) {
        return new MyTodayAttendanceResponse(
                view.date(), view.workStartTime(), view.workEndTime(), view.clockInAt(),
                view.clockOutAt(), view.status(), view.serverTime());
    }
}
