package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

@Component
public class MyAttendanceStatusResolver {

    public MyAttendanceDayStatus resolve(
            LocalDate date,
            MyAttendanceScheduleResolver.WorkSchedule schedule,
            AttendanceRecord record,
            Collection<LeaveRequest> approvedLeaves,
            LocalDate today,
            LocalTime currentTime) {
        if (!schedule.workday()) {
            return MyAttendanceDayStatus.OFF;
        }
        if (record != null) {
            return MyAttendanceDayStatus.valueOf(record.getStatus().name());
        }
        if (approvedLeaves.stream().anyMatch(leave -> leaveCovers(leave, date))) {
            return MyAttendanceDayStatus.LEAVE;
        }
        if (date.isBefore(today)
                || (date.equals(today) && !currentTime.isBefore(schedule.endTime()))) {
            return MyAttendanceDayStatus.ABSENT;
        }
        return MyAttendanceDayStatus.UNRECORDED;
    }

    private boolean leaveCovers(LeaveRequest leave, LocalDate date) {
        return !date.isBefore(leave.getStartDate()) && !date.isAfter(leave.getEndDate());
    }
}
