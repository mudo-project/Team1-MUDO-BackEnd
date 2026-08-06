package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface LeaveGrantJpaRepository extends JpaRepository<LeaveGrantJpaEntity, Long> {

    boolean existsByAcademyIdAndUserIdAndGrantDate(Long academyId, Long userId, LocalDate grantDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from LeaveGrantJpaEntity g "
            + "where g.academyId = :academyId and g.userId = :userId "
            + "and g.grantDate <= :date and g.expirationDate >= :date")
    Optional<LeaveGrantJpaEntity> findActiveForUpdate(@Param("academyId") Long academyId,
                                                       @Param("userId") Long userId,
                                                       @Param("date") LocalDate date);
}
