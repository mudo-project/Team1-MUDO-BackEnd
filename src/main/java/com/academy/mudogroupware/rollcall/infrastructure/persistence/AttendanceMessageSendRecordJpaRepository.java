package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public interface AttendanceMessageSendRecordJpaRepository
        extends JpaRepository<AttendanceMessageSendRecordEntity, Long> {

    Optional<AttendanceMessageSendRecordEntity> findByLectureIdAndStudentIdAndDateAndAttendanceStatus(
            Long lectureId, Long studentId, LocalDate date, AttendanceStatus attendanceStatus);

    @Modifying(clearAutomatically = true)
    @Query("update AttendanceMessageSendRecordEntity e set e.status = :sending "
            + "where e.id = :id and e.status in :claimableStatuses")
    int claimForSending(@Param("id") Long id, @Param("sending") AttendanceMessageSendStatus sending,
                        @Param("claimableStatuses") List<AttendanceMessageSendStatus> claimableStatuses);
}
