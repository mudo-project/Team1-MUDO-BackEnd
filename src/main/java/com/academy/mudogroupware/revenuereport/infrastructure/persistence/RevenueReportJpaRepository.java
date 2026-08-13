package com.academy.mudogroupware.revenuereport.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevenueReportJpaRepository extends JpaRepository<RevenueReportEntity, Long> {

    Optional<RevenueReportEntity> findByTargetMonth(LocalDate targetMonth);

    List<RevenueReportEntity> findAllByOrderByTargetMonthDesc();

    long countByReadAtIsNull();
}
