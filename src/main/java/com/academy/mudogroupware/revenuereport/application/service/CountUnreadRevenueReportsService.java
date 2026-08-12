package com.academy.mudogroupware.revenuereport.application.service;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.revenuereport.application.usecase.CountUnreadRevenueReportsUseCase;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountUnreadRevenueReportsService implements CountUnreadRevenueReportsUseCase {

    private final RevenueReportRepository revenueReportRepository;

    @Override
    public long countUnread() {
        long count = revenueReportRepository.countUnread();
        log.info("event=revenue_report_unread_count_조회 count={}", count);
        return count;
    }
}
