package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;

public interface AttendanceRecordRepository {
    boolean existsByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate);

    AttendanceRecord save(AttendanceRecord attendanceRecord);

    Optional<AttendanceRecord> findByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate);

    Optional<AttendanceRecord> findLatestOpenSince(
            Long academyId, Long userId, LocalDate earliestWorkDate);

    boolean existsCheckedOutBetween(
            Long academyId, Long userId, LocalDateTime from, LocalDateTime to);

    Optional<AttendanceRecord> findByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate);

    List<AttendanceRecord> findByAcademyIdAndUserIdAndWorkDateBetween(
            Long academyId, Long userId, LocalDate startDate, LocalDate endDate);
}
