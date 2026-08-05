package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceEntryJpaRepository extends JpaRepository<AttendanceEntryEntity, Long> {

    Optional<AttendanceEntryEntity> findByLectureIdAndStudentIdAndDate(Long lectureId, Long studentId, LocalDate date);

    List<AttendanceEntryEntity> findAllByLectureIdAndDate(Long lectureId, LocalDate date);
}
