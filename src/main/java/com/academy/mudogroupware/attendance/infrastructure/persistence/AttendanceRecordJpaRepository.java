package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordJpaRepository
        extends JpaRepository<AttendanceRecordJpaEntity, Long> {
    boolean existsByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate);
}
