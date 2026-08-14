package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AttendanceRecordJpaRepository
        extends JpaRepository<AttendanceRecordJpaEntity, Long> {
    boolean existsByUserIdAndWorkDate(
            Long userId, LocalDate workDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AttendanceRecordJpaEntity>
            findFirstByUserIdAndWorkDateGreaterThanEqualAndClockOutAtIsNullOrderByClockInAtDesc(
                    Long userId, LocalDate earliestWorkDate);

    boolean existsByUserIdAndClockOutAtBetween(
            Long userId, LocalDateTime from, LocalDateTime to);

    Optional<AttendanceRecordJpaEntity> findByUserIdAndWorkDate(
            Long userId, LocalDate workDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AttendanceRecordJpaEntity a "
            + "where a.userId = :userId and a.workDate = :workDate")
    Optional<AttendanceRecordJpaEntity> findByUserIdAndWorkDateForUpdate(
            @Param("userId") Long userId, @Param("workDate") LocalDate workDate);

    List<AttendanceRecordJpaEntity> findAllByUserIdAndWorkDateBetweenOrderByWorkDate(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<AttendanceRecordJpaEntity> findAllByUserIdInAndWorkDate(
            List<Long> userIds, LocalDate workDate);
}
