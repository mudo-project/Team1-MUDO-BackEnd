package com.academy.mudogroupware.revenuereport.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.revenuereport.application.usecase.GetRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportNotFoundException;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetRevenueReportService implements GetRevenueReportUseCase {

    private final RevenueReportRepository revenueReportRepository;
    private final Clock clock;

    @Override
    public RevenueReport getReport(Long reportId) {
        log.info("event=revenue_report_get_시작 reportId={}", reportId);
        try {
            RevenueReport report = revenueReportRepository.findById(reportId)
                    .orElseThrow(RevenueReportNotFoundException::new);
            revenueReportRepository.markRead(reportId, LocalDateTime.now(clock));
            log.info("event=revenue_report_get_완료 reportId={}", reportId);
            return report;
        } catch (RuntimeException e) {
            log.warn("event=revenue_report_get_실패 reportId={}, reason={}", reportId, e.getMessage(), e);
            throw e;
        }
    }
}
