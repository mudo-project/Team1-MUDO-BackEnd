package com.academy.mudogroupware.attendance.application.port;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;

public record WeeklyEmployeeDetail(
        Long userId,
        String name,
        String position,
        LocalDate workDate,
        LocalDateTime clockInAt,
        LocalDateTime clockOutAt,
        AttendanceStatus attendanceStatus) {
}
