package com.academy.mudogroupware.revenuereport.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportNotFoundException;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RevenueReportRepositoryImpl implements RevenueReportRepository {

    private final RevenueReportJpaRepository revenueReportJpaRepository;

    @Override
    @Transactional
    public RevenueReport save(RevenueReport report) {
        RevenueReportEntity entity = RevenueReportEntity.builder()
                .targetMonth(report.getTargetMonth())
                .report(report.getReport())
                .dataSnapshot(report.getDataSnapshot())
                .build();
        RevenueReportEntity saved = revenueReportJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RevenueReport> findByTargetMonth(LocalDate targetMonth) {
        return revenueReportJpaRepository.findByTargetMonth(targetMonth).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RevenueReport> findById(Long reportId) {
        return revenueReportJpaRepository.findById(reportId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueReport> findAllOrderByTargetMonthDesc() {
        return revenueReportJpaRepository.findAllByOrderByTargetMonthDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread() {
        return revenueReportJpaRepository.countByReadAtIsNull();
    }

    @Override
    @Transactional
    public void markRead(Long reportId, LocalDateTime now) {
        RevenueReportEntity entity = revenueReportJpaRepository.findById(reportId)
                .orElseThrow(RevenueReportNotFoundException::new);
        if (entity.getReadAt() == null) {
            entity.setReadAt(now);
        }
    }

    private RevenueReport toDomain(RevenueReportEntity entity) {
        return RevenueReport.restore(entity.getId(), entity.getTargetMonth(), entity.getReport(),
                entity.getDataSnapshot(), entity.getReadAt(), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
