package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;

public interface AttendanceRecordRepository {
    boolean existsByUserIdAndWorkDate(
            Long userId, LocalDate workDate);

    AttendanceRecord save(AttendanceRecord attendanceRecord);

    Optional<AttendanceRecord> findLatestOpenSince(
            Long userId, LocalDate earliestWorkDate);

    boolean existsCheckedOutBetween(
            Long userId, LocalDateTime from, LocalDateTime to);

    Optional<AttendanceRecord> findByUserIdAndWorkDate(
            Long userId, LocalDate workDate);

    List<AttendanceRecord> findByUserIdAndWorkDateBetween(
            Long userId, LocalDate startDate, LocalDate endDate);

    List<AttendanceRecord> findAllByUserIdsAndWorkDate(
            List<Long> userIds, LocalDate workDate);
}
