package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

public record WeeklyEmployeeAttendanceView(
        LocalDate startDate,
        LocalDate endDate,
        int scheduledWorkDays,
        PageResult<Employee> employees) {

    public record Employee(
            Long userId,
            String name,
            String roleName,
            int attendedDays,
            int scheduledWorkDays,
            List<Day> days) {
    }

    public record Day(
            LocalDate date,
            MyAttendanceDayStatus status,
            LocalDateTime clockInAt,
            LocalDateTime clockOutAt) {
    }
}
