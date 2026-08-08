package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;

public interface AttendanceCorrectionRequestJpaRepository extends JpaRepository<AttendanceCorrectionRequestJpaEntity, Long> {
    boolean existsByAcademyIdAndUserIdAndWorkDateAndStatus(Long academyId, Long userId, LocalDate workDate, AttendanceCorrectionStatus status);
    Optional<AttendanceCorrectionRequestJpaEntity> findByIdAndAcademyIdAndUserId(Long id, Long academyId, Long userId);
    Optional<AttendanceCorrectionRequestJpaEntity> findByIdAndAcademyId(Long id, Long academyId);
    List<AttendanceCorrectionRequestJpaEntity> findAllByAcademyIdAndUserIdOrderByRequestedAtDesc(Long academyId, Long userId);
    Slice<AttendanceCorrectionRequestJpaEntity> findAllByAcademyId(Long academyId, Pageable pageable);
    Slice<AttendanceCorrectionRequestJpaEntity> findAllByAcademyIdAndStatus(Long academyId, AttendanceCorrectionStatus status, Pageable pageable);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AttendanceCorrectionRequestJpaEntity r where r.id = :id and r.academyId = :academyId")
    Optional<AttendanceCorrectionRequestJpaEntity> findByIdAndAcademyIdForUpdate(
            @Param("id") Long id, @Param("academyId") Long academyId);
}
