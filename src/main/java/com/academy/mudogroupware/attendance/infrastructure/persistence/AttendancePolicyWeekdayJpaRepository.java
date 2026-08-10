package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendancePolicyWeekdayJpaRepository
        extends JpaRepository<AttendancePolicyWeekdayJpaEntity, AttendancePolicyWeekdayId> {
    List<AttendancePolicyWeekdayJpaEntity> findAllByIdPolicyIdOrderByIdDayOfWeek(Long policyId);
    void deleteAllByIdPolicyId(Long policyId);
}
