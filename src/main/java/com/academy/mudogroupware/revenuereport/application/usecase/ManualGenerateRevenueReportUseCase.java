package com.academy.mudogroupware.revenuereport.application.usecase;

import java.time.LocalDate;

public interface ManualGenerateRevenueReportUseCase {

    void generateManually(LocalDate targetMonth);
}
