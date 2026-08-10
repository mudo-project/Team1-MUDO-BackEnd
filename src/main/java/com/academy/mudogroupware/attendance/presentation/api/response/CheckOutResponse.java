package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.application.result.CheckOutResult;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.model.ClockOutType;

public record CheckOutResponse(
        Long attendanceId,
        LocalDate workDate,
        LocalDateTime clockInAt,
        LocalDateTime clockOutAt,
        ClockOutType clockOutType,
        String clockOutNote,
        AttendanceStatus status
) {
    public static CheckOutResponse from(CheckOutResult result) {
        return new CheckOutResponse(
                result.attendanceId(), result.workDate(), result.clockInAt(),
                result.clockOutAt(), result.clockOutType(),
                result.clockOutNote(), result.status());
    }
}
