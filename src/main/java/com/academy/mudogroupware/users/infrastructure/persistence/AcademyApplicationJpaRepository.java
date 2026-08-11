package com.academy.mudogroupware.users.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;

public interface AcademyApplicationJpaRepository extends JpaRepository<AcademyApplicationEntity, Long> {

    boolean existsByRequestedLoginIdAndStatusIn(String requestedLoginId, List<AcademyApplicationStatus> statuses);

    @Modifying(clearAutomatically = true)
    @Query("update AcademyApplicationEntity a set a.status = com.academy.mudogroupware.users.domain.model."
            + "AcademyApplicationStatus.APPROVED, a.reviewedByUserId = :reviewerId, a.reviewedAt = :reviewedAt, "
            + "a.updatedAt = :reviewedAt "
            + "where a.id = :id and a.status = com.academy.mudogroupware.users.domain.model."
            + "AcademyApplicationStatus.PENDING")
    int markApprovedIfPending(@Param("id") Long id, @Param("reviewerId") Long reviewerId,
                               @Param("reviewedAt") LocalDateTime reviewedAt);
}
