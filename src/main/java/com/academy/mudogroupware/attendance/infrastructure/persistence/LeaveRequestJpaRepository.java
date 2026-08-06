package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;

public interface LeaveRequestJpaRepository extends JpaRepository<LeaveRequestJpaEntity, Long> {

    Optional<LeaveRequestJpaEntity> findByDocumentId(Long documentId);

    @Query("select l.userId from LeaveRequestJpaEntity l "
            + "where l.academyId = :academyId and l.status = :status "
            + "and :date between l.startDate and l.endDate")
    List<Long> findUserIdsByAcademyIdAndStatusAndDateBetween(@Param("academyId") Long academyId,
                                                              @Param("status") LeaveRequestStatus status,
                                                              @Param("date") LocalDate date);
}
