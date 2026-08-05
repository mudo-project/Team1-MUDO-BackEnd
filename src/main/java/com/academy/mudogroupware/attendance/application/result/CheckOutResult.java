package com.academy.mudogroupware.attendance.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;

public record CheckOutResult(
        Long attendanceId,
        LocalDate workDate,
        LocalDateTime clockInAt,
        LocalDateTime clockOutAt,
        String clockOutNote,
        AttendanceStatus status
) {
    public static CheckOutResult from(AttendanceRecord record) {
        return new CheckOutResult(
                record.getId(), record.getWorkDate(), record.getClockInAt(),
                record.getClockOutAt(), record.getClockOutNote(), record.getStatus());
    }
}
