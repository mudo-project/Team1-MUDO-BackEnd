package com.academy.mudogroupware.attendance.application.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;

public record CheckInResult(
        Long attendanceId,
        LocalDate workDate,
        LocalDateTime clockInAt,
        String clockInNote,
        AttendanceStatus status
) {
    public static CheckInResult from(AttendanceRecord record) {
        return new CheckInResult(
                record.getId(), record.getWorkDate(), record.getClockInAt(),
                record.getClockInNote(), record.getStatus());
    }
}
