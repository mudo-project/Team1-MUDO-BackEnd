package com.academy.mudogroupware.attendance.application.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;

@Component
public class LeaveUsedDaysCalculator {

    public int calculate(AttendancePolicy policy, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_LEAVE_PERIOD);
        }
        int usedDays = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (policy.isWorkday(date.getDayOfWeek().getValue())) {
                usedDays++;
            }
        }
        if (usedDays == 0) {
            throw new AttendanceException(AttendanceErrorCode.LEAVE_REQUEST_NO_WORKDAY);
        }
        return usedDays;
    }
}
