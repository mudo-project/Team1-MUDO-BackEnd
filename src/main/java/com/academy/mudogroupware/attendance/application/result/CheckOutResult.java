package com.academy.mudogroupware.attendance.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.model.ClockOutType;

public record CheckOutResult(
        Long attendanceId,
        LocalDate workDate,
        LocalDateTime clockInAt,
        LocalDateTime clockOutAt,
        ClockOutType clockOutType,
        String clockOutNote,
        AttendanceStatus status
) {
    public static CheckOutResult from(AttendanceRecord record) {
        return new CheckOutResult(
                record.getId(), record.getWorkDate(), record.getClockInAt(),
                record.getClockOutAt(), record.getClockOutType(),
                record.getClockOutNote(), record.getStatus());
    }
}
