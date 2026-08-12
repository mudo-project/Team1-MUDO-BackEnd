package com.academy.mudogroupware.revenuereport.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;

public interface RevenueReportRepository {

    RevenueReport save(RevenueReport report);

    Optional<RevenueReport> findByTargetMonth(LocalDate targetMonth);

    Optional<RevenueReport> findById(Long reportId);

    List<RevenueReport> findAllOrderByTargetMonthDesc();

    long countUnread();

    void markRead(Long reportId, java.time.LocalDateTime now);
}
