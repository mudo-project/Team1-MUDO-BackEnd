package com.academy.mudogroupware.rollcall.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceEntry;

public interface AttendanceEntryRepository {

    Optional<AttendanceEntry> findByLectureIdAndStudentIdAndDate(Long lectureId, Long studentId, LocalDate date);

    List<AttendanceEntry> findByLectureIdAndDate(Long lectureId, LocalDate date);

    AttendanceEntry save(AttendanceEntry entry);
}
