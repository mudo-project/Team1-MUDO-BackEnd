package com.academy.mudogroupware.resourceusage.presentation.api.response;

import java.util.List;

import com.academy.mudogroupware.resourceusage.application.query.MonthlyResourceUsageView;

public record MonthlyResourceUsageResponse(
        String month,
        List<ResourceUsageResourceSummaryResponse> resources
) {

    public static MonthlyResourceUsageResponse from(MonthlyResourceUsageView view) {
        return new MonthlyResourceUsageResponse(
                view.month().toString(),
                view.resources().stream()
                        .map(ResourceUsageResourceSummaryResponse::from)
                        .toList());
    }
}
