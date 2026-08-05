package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;

public interface AttendanceRecordRepository {
    boolean existsByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate);

    AttendanceRecord save(AttendanceRecord attendanceRecord);

    Optional<AttendanceRecord> findLatestOpenSince(
            Long academyId, Long userId, LocalDate earliestWorkDate);

    boolean existsCheckedOutBetween(
            Long academyId, Long userId, LocalDateTime from, LocalDateTime to);
}
