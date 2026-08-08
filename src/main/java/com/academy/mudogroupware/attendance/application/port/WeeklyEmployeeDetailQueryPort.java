package com.academy.mudogroupware.attendance.application.port;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyEmployeeDetailQueryPort {
    List<WeeklyEmployeeDetail> findByEmployee(
            Long academyId, Long userId, LocalDate startDate, LocalDate endDate);
}
