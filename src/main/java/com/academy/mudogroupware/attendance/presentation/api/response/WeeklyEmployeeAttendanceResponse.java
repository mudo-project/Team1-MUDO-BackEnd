package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.attendance.application.query.WeeklyEmployeeAttendanceView;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;
import com.academy.mudogroupware.global.presentation.api.common.PageResponse;

public record WeeklyEmployeeAttendanceResponse(
        Week week,
        int scheduledWorkDays,
        PageResponse<Employee> employees) {

    public static WeeklyEmployeeAttendanceResponse from(WeeklyEmployeeAttendanceView view) {
        return new WeeklyEmployeeAttendanceResponse(
                new Week(view.startDate(), view.endDate()),
                view.scheduledWorkDays(),
                PageResponse.from(view.employees(), Employee::from));
    }

    public record Week(LocalDate startDate, LocalDate endDate) {
    }

    public record Employee(
            Long userId,
            String name,
            String roleName,
            int attendedDays,
            int scheduledWorkDays,
            List<Day> days) {
        private static Employee from(WeeklyEmployeeAttendanceView.Employee employee) {
            return new Employee(employee.userId(), employee.name(), employee.roleName(), employee.attendedDays(),
                    employee.scheduledWorkDays(), employee.days().stream().map(Day::from).toList());
        }
    }

    public record Day(
            LocalDate date,
            MyAttendanceDayStatus status,
            LocalDateTime clockInAt,
            LocalDateTime clockOutAt) {
        private static Day from(WeeklyEmployeeAttendanceView.Day day) {
            return new Day(day.date(), day.status(), day.clockInAt(), day.clockOutAt());
        }
    }
}
