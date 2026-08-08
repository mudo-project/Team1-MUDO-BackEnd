package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

public record WeeklyEmployeeDetailView(
        Employee employee,
        LocalDate startDate,
        LocalDate endDate,
        int scheduledWorkDays,
        int attendedDays,
        List<Day> days) {

    public record Employee(Long userId, String name, String position) {
    }

    public record Day(LocalDate date, MyAttendanceDayStatus status,
                      LocalDateTime clockInAt, LocalDateTime clockOutAt) {
    }
}
