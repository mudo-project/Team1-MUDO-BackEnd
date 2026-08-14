package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public interface AttendanceMessageSendRecordJpaRepository
        extends JpaRepository<AttendanceMessageSendRecordEntity, Long> {

    Optional<AttendanceMessageSendRecordEntity> findByLectureIdAndStudentIdAndDateAndAttendanceStatus(
            Long lectureId, Long studentId, LocalDate date, AttendanceStatus attendanceStatus);

    // 호출부(SendAttendanceMessagesService)가 @Transactional이 아니라서, Spring Data
    // 리포지토리 프록시의 기본값(읽기전용)으로 실행되면 이 UPDATE가 실패할 수 있다 — 직접 쓰기
    // 트랜잭션을 명시한다. SOLAPI 호출은 이 트랜잭션 밖(claimForSending 리턴 이후)에서 실행된다.
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update AttendanceMessageSendRecordEntity e set e.status = :sending, e.claimedAt = :now "
            + "where e.id = :id and e.status in :claimableStatuses")
    int claimForSending(@Param("id") Long id, @Param("sending") AttendanceMessageSendStatus sending,
                        @Param("claimableStatuses") List<AttendanceMessageSendStatus> claimableStatuses,
                        @Param("now") LocalDateTime now);
}
