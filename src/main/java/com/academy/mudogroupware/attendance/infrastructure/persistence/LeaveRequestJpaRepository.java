package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;

public interface LeaveRequestJpaRepository extends JpaRepository<LeaveRequestJpaEntity, Long> {

    Optional<LeaveRequestJpaEntity> findByDocumentId(Long documentId);

    @Query("select l.userId from LeaveRequestJpaEntity l "
            + "where l.status = :status "
            + "and :date between l.startDate and l.endDate")
    List<Long> findUserIdsByStatusAndDateBetween(@Param("status") LeaveRequestStatus status,
                                                              @Param("date") LocalDate date);

    @Query("select l from LeaveRequestJpaEntity l "
            + "where l.status = :status "
            + "and l.startDate <= :endDate and l.endDate >= :startDate")
    List<LeaveRequestJpaEntity> findAllOverlapping(
            @Param("status") LeaveRequestStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("select (count(l) > 0) from LeaveRequestJpaEntity l "
            + "where l.userId = :userId and l.status in :statuses "
            + "and l.startDate <= :endDate and l.endDate >= :startDate")
    boolean existsOverlapping(@Param("userId") Long userId,
                              @Param("statuses") Set<LeaveRequestStatus> statuses,
                              @Param("startDate") LocalDate startDate,
                              @Param("endDate") LocalDate endDate);

    @Query("select coalesce(sum(l.usedDays), 0) from LeaveRequestJpaEntity l "
            + "where l.userId = :userId and l.status in :statuses "
            + "and l.startDate >= :periodStart and l.endDate <= :periodEnd")
    int sumUsedDays(@Param("userId") Long userId,
                    @Param("statuses") Set<LeaveRequestStatus> statuses,
                    @Param("periodStart") LocalDate periodStart,
                    @Param("periodEnd") LocalDate periodEnd);

    @Query("select l from LeaveRequestJpaEntity l "
            + "where l.userId = :userId and l.status = :status "
            + "and l.startDate <= :endDate and l.endDate >= :startDate")
    List<LeaveRequestJpaEntity> findOverlapping(
            @Param("userId") Long userId,
            @Param("status") LeaveRequestStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
