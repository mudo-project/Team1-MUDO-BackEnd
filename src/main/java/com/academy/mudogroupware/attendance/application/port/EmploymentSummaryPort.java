package com.academy.mudogroupware.attendance.application.port;

import java.time.LocalDate;
import java.util.Optional;

public interface EmploymentSummaryPort {
    Optional<EmploymentSummary> findByUserIdAndAcademyId(Long userId, Long academyId);

    record EmploymentSummary(LocalDate hireDate) {
    }
}
