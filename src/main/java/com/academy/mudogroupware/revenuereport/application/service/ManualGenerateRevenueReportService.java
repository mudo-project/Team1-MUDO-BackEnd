package com.academy.mudogroupware.revenuereport.application.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.academy.mudogroupware.revenuereport.application.usecase.GenerateRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.application.usecase.ManualGenerateRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportAlreadyExistsException;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

import lombok.RequiredArgsConstructor;

/**
 * 배치 전용 {@link GenerateRevenueReportUseCase}는 이미 있으면 조용히 스킵하지만,
 * 이 서비스는 사람이 직접 호출하는 API용이라 이미 있으면 예외로 명확히 알린다.
 * 생성 로직 자체(집계·AI 서술·저장·알림)는 전부 위임해 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class ManualGenerateRevenueReportService implements ManualGenerateRevenueReportUseCase {

    private final RevenueReportRepository revenueReportRepository;
    private final GenerateRevenueReportUseCase generateRevenueReportUseCase;

    @Override
    public void generateManually(LocalDate targetMonth) {
        if (revenueReportRepository.findByTargetMonth(targetMonth).isPresent()) {
            throw new RevenueReportAlreadyExistsException();
        }
        generateRevenueReportUseCase.generate(targetMonth);
    }
}
