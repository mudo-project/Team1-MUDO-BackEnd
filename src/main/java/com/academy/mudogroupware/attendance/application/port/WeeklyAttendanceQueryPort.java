package com.academy.mudogroupware.attendance.application.port;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyAttendanceQueryPort {
    List<WeeklyAttendanceEmployee> findEmployees(
            Long ownerUserId, LocalDate startDate, LocalDate endDate);
}
