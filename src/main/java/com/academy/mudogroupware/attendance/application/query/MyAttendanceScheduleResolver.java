package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;

@Component
public class MyAttendanceScheduleResolver {

    public WorkSchedule resolve(AttendancePolicy policy, LocalDate date) {
        if (!policy.isWeekdayExceptionEnabled()) {
            return defaultSchedule(policy, true);
        }
        return policy.getWeekdays().stream()
                .filter(weekday -> weekday.dayOfWeek() == date.getDayOfWeek().getValue())
                .findFirst()
                .map(weekday -> fromWeekday(policy, weekday))
                .orElseGet(() -> defaultSchedule(policy, true));
    }

    private WorkSchedule fromWeekday(
            AttendancePolicy policy, AttendancePolicyWeekday weekday) {
        if (!weekday.workday()) {
            return defaultSchedule(policy, false);
        }
        return new WorkSchedule(
                true,
                weekday.startTime() == null
                        ? policy.getDefaultStartTime() : weekday.startTime(),
                weekday.endTime() == null
                        ? policy.getDefaultEndTime() : weekday.endTime());
    }

    private WorkSchedule defaultSchedule(AttendancePolicy policy, boolean workday) {
        return new WorkSchedule(
                workday, policy.getDefaultStartTime(), policy.getDefaultEndTime());
    }

    public record WorkSchedule(boolean workday, LocalTime startTime, LocalTime endTime) {
    }
}
