package com.academy.mudogroupware.revenuereport.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.revenuereport.application.usecase.ListRevenueReportsUseCase;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListRevenueReportsService implements ListRevenueReportsUseCase {

    private final RevenueReportRepository revenueReportRepository;

    @Override
    public List<RevenueReport> listReports() {
        log.info("event=revenue_report_list_시작");
        List<RevenueReport> result = revenueReportRepository.findAllOrderByTargetMonthDesc();
        log.info("event=revenue_report_list_완료 count={}", result.size());
        return result;
    }
}
