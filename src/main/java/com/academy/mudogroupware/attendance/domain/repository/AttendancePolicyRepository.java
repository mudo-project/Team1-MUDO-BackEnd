package com.academy.mudogroupware.attendance.domain.repository;

import java.util.Optional;

import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;

public interface AttendancePolicyRepository {
    Optional<AttendancePolicy> findByAcademyId(Long academyId);
    AttendancePolicy save(AttendancePolicy policy);
}
