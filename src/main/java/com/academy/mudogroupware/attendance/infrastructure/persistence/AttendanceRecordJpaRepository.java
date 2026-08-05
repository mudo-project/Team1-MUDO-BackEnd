package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;

public interface AttendanceRecordJpaRepository
        extends JpaRepository<AttendanceRecordJpaEntity, Long> {
    boolean existsByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AttendanceRecordJpaEntity>
            findFirstByAcademyIdAndUserIdAndWorkDateGreaterThanEqualAndClockOutAtIsNullOrderByClockInAtDesc(
                    Long academyId, Long userId, LocalDate earliestWorkDate);

    boolean existsByAcademyIdAndUserIdAndClockOutAtBetween(
            Long academyId, Long userId, LocalDateTime from, LocalDateTime to);
}
