package com.academy.mudogroupware.resourceusage.application.query;

import java.time.YearMonth;
import java.util.List;

public record MonthlyResourceUsageView(
        YearMonth month,
        List<ResourceUsageResourceSummaryView> resources
) {
}
