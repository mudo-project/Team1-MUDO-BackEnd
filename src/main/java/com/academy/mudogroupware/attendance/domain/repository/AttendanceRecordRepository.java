package com.academy.mudogroupware.attendance.domain.repository;

import java.time.LocalDate;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;

public interface AttendanceRecordRepository {
    boolean existsByAcademyIdAndUserIdAndWorkDate(
            Long academyId, Long userId, LocalDate workDate);

    AttendanceRecord save(AttendanceRecord attendanceRecord);
}
