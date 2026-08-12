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
            LocalDateTime now = LocalDateTime.now(clock);
            revenueReportRepository.markRead(reportId, now);
            // 위 markRead()는 DB(엔티티)만 갱신한다 — 지금 들고 있는 도메인 객체에도 반영해야
            // 이 메서드가 돌려주는 값의 readAt이 방금 저장한 DB 상태와 일치한다. markRead는
            // readAt이 이미 있으면 덮어쓰지 않으므로(최초 읽음 시각 유지) 재조회 시에도 안전하다.
            report.markRead(now);
            log.info("event=revenue_report_get_완료 reportId={}", reportId);
            return report;
        } catch (RuntimeException e) {
            log.warn("event=revenue_report_get_실패 reportId={}, reason={}", reportId, e.getMessage(), e);
            throw e;
        }
    }
}
