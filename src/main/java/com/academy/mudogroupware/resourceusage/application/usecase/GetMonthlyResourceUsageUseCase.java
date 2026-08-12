package com.academy.mudogroupware.resourceusage.application.usecase;

import java.time.YearMonth;

import com.academy.mudogroupware.resourceusage.application.query.MonthlyResourceUsageView;

public interface GetMonthlyResourceUsageUseCase {

    MonthlyResourceUsageView getMonthlyUsage(YearMonth month);
}
