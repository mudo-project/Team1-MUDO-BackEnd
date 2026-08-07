package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;

public interface AttendanceCorrectionRequestJpaRepository extends JpaRepository<AttendanceCorrectionRequestJpaEntity, Long> {
    boolean existsByAcademyIdAndUserIdAndWorkDateAndStatus(Long academyId, Long userId, LocalDate workDate, AttendanceCorrectionStatus status);
    Optional<AttendanceCorrectionRequestJpaEntity> findByIdAndAcademyIdAndUserId(Long id, Long academyId, Long userId);
    List<AttendanceCorrectionRequestJpaEntity> findAllByAcademyIdAndUserIdOrderByRequestedAtDesc(Long academyId, Long userId);
}
