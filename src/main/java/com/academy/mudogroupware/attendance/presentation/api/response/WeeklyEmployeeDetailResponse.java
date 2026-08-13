package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.attendance.application.query.WeeklyEmployeeDetailView;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

public record WeeklyEmployeeDetailResponse(
        Employee employee, Week week, List<Day> days, WeeklySummary weeklySummary) {

    public static WeeklyEmployeeDetailResponse from(WeeklyEmployeeDetailView view) {
        return new WeeklyEmployeeDetailResponse(
                new Employee(view.employee().userId(), view.employee().name(), view.employee().roleName()),
                new Week(view.startDate(), view.endDate()), view.days().stream().map(Day::from).toList(),
                new WeeklySummary(view.scheduledWorkDays(), view.attendedDays()));
    }

    public record Employee(Long userId, String name, String roleName) {}
    public record Week(LocalDate startDate, LocalDate endDate) {}
    public record Day(LocalDate date, MyAttendanceDayStatus status,
                      LocalDateTime clockInAt, LocalDateTime clockOutAt) {
        private static Day from(WeeklyEmployeeDetailView.Day day) {
            return new Day(day.date(), day.status(), day.clockInAt(), day.clockOutAt());
        }
    }
    public record WeeklySummary(int scheduledWorkDays, int attendedDays) {}
}
