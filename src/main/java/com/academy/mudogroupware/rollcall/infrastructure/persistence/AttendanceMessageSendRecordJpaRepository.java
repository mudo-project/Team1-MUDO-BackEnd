package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceMessageSendRecordJpaRepository
        extends JpaRepository<AttendanceMessageSendRecordEntity, Long> {

    Optional<AttendanceMessageSendRecordEntity> findByLectureIdAndStudentIdAndDate(Long lectureId, Long studentId,
                                                                                    LocalDate date);
}
