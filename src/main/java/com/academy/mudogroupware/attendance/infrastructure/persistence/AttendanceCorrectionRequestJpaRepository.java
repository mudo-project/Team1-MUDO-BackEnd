package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;

public interface AttendanceCorrectionRequestJpaRepository extends JpaRepository<AttendanceCorrectionRequestJpaEntity, Long> {
    boolean existsByUserIdAndWorkDateAndStatus(Long userId, LocalDate workDate, AttendanceCorrectionStatus status);
    Optional<AttendanceCorrectionRequestJpaEntity> findByIdAndUserId(Long id, Long userId);
    Optional<AttendanceCorrectionRequestJpaEntity> findById(Long id);
    List<AttendanceCorrectionRequestJpaEntity> findAllByUserIdOrderByRequestedAtDesc(Long userId);
    @Query("select r from AttendanceCorrectionRequestJpaEntity r")
    Page<AttendanceCorrectionRequestJpaEntity> findPage(Pageable pageable);
    Page<AttendanceCorrectionRequestJpaEntity> findAllByStatus(AttendanceCorrectionStatus status, Pageable pageable);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AttendanceCorrectionRequestJpaEntity r where r.id = :id")
    Optional<AttendanceCorrectionRequestJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
