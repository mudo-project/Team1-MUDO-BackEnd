package com.academy.mudogroupware.attendance.application.port;

import java.time.LocalDate;
import java.util.Optional;

public interface EmploymentSummaryPort {
    Optional<EmploymentSummary> findByUserId(Long userId);

    record EmploymentSummary(LocalDate hireDate) {
    }
}
