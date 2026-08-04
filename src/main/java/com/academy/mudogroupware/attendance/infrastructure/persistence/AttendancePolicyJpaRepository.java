package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendancePolicyJpaRepository
        extends JpaRepository<AttendancePolicyJpaEntity, Long> {
    Optional<AttendancePolicyJpaEntity> findByAcademyId(Long academyId);
}
