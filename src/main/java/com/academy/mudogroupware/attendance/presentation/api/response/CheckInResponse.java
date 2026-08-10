package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.application.result.CheckInResult;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;

public record CheckInResponse(
        Long attendanceId,
        LocalDate workDate,
        LocalDateTime clockInAt,
        String clockInNote,
        AttendanceStatus status
) {
    public static CheckInResponse from(CheckInResult result) {
        return new CheckInResponse(
                result.attendanceId(), result.workDate(), result.clockInAt(),
                result.clockInNote(), result.status());
    }
}
