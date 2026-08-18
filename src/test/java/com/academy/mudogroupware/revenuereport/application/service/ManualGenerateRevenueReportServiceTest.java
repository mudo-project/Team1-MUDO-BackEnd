package com.academy.mudogroupware.revenuereport.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.application.usecase.GenerateRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportAlreadyExistsException;
import com.academy.mudogroupware.revenuereport.domain.model.RevenueReport;
import com.academy.mudogroupware.revenuereport.domain.repository.RevenueReportRepository;

class ManualGenerateRevenueReportServiceTest {

    private final RevenueReportRepository revenueReportRepository = mock(RevenueReportRepository.class);
    private final GenerateRevenueReportUseCase generateRevenueReportUseCase = mock(GenerateRevenueReportUseCase.class);
    private final ManualGenerateRevenueReportService service =
            new ManualGenerateRevenueReportService(revenueReportRepository, generateRevenueReportUseCase);

    @Test
    void throwsWhenReportAlreadyExistsForTargetMonth() {
        LocalDate targetMonth = LocalDate.of(2026, 7, 1);
        RevenueReport existing = RevenueReport.create(
                targetMonth, "이미 있음", "{}", java.time.LocalDateTime.now());
        when(revenueReportRepository.findByTargetMonth(targetMonth)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.generateManually(targetMonth))
                .isInstanceOf(RevenueReportAlreadyExistsException.class);

        verify(generateRevenueReportUseCase, never()).generate(targetMonth);
    }

    @Test
    void delegatesToGenerateUseCaseWhenNotExists() {
        LocalDate targetMonth = LocalDate.of(2026, 7, 1);
        when(revenueReportRepository.findByTargetMonth(targetMonth)).thenReturn(Optional.empty());

        service.generateManually(targetMonth);

        verify(generateRevenueReportUseCase).generate(targetMonth);
    }
}
