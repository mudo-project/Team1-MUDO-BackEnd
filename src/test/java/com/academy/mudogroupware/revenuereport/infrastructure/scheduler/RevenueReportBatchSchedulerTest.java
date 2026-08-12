package com.academy.mudogroupware.revenuereport.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.revenuereport.application.usecase.GenerateRevenueReportUseCase;
import com.academy.mudogroupware.revenuereport.domain.exception.RevenueReportAiException;

class RevenueReportBatchSchedulerTest {

    private final GenerateRevenueReportUseCase generateRevenueReportUseCase = mock(GenerateRevenueReportUseCase.class);
    private final RevenueReportBatchScheduler scheduler =
            new RevenueReportBatchScheduler(generateRevenueReportUseCase);

    @Test
    void retriesLastThreeMonthsEveryRun() {
        LocalDate expectedLatest = LocalDate.now().minusMonths(1).withDayOfMonth(1);

        scheduler.generateMonthlyReport();

        verify(generateRevenueReportUseCase).generate(eq(expectedLatest));
        verify(generateRevenueReportUseCase).generate(eq(expectedLatest.minusMonths(1)));
        verify(generateRevenueReportUseCase).generate(eq(expectedLatest.minusMonths(2)));
    }

    @Test
    void oneMonthFailureDoesNotStopOtherMonthsFromBeingRetried() {
        LocalDate expectedLatest = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        doThrow(new RevenueReportAiException("AI 서버 호출 실패"))
                .when(generateRevenueReportUseCase).generate(expectedLatest);

        scheduler.generateMonthlyReport();

        verify(generateRevenueReportUseCase).generate(eq(expectedLatest.minusMonths(1)));
        verify(generateRevenueReportUseCase).generate(eq(expectedLatest.minusMonths(2)));
    }
}
