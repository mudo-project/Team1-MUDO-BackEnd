package com.academy.mudogroupware.attendance.application.port;

import java.time.LocalDate;
import java.util.List;

public interface TeamAttendanceQueryPort {
    List<TeamAttendanceEmployee> findEmployeesWithAttendance(
            Long ownerUserId, LocalDate workDate);
}
