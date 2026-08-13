package com.academy.mudogroupware.revenuereport.application.usecase;

import java.time.LocalDate;

public interface GenerateRevenueReportUseCase {

    void generate(LocalDate targetMonth);
}
