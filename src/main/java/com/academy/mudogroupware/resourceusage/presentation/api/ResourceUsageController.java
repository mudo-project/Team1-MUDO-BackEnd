package com.academy.mudogroupware.resourceusage.presentation.api;

import java.time.YearMonth;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.resourceusage.application.query.MonthlyResourceUsageView;
import com.academy.mudogroupware.resourceusage.application.usecase.GetMonthlyResourceUsageUseCase;
import com.academy.mudogroupware.resourceusage.presentation.api.common.ResourceUsageResponseCode;
import com.academy.mudogroupware.resourceusage.presentation.api.response.MonthlyResourceUsageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Resource Usage", description = "Academy monthly AI token and SMS usage API")
@RestController
@RequestMapping("/api/resource-usage")
@RequiredArgsConstructor
public class ResourceUsageController {

    private final GetMonthlyResourceUsageUseCase getMonthlyResourceUsageUseCase;

    @Operation(summary = "Monthly resource usage",
            description = "Returns monthly AI token and SMS usage grouped by resource type and feature.")
    @PreAuthorize("hasAuthority('ACCOUNT:MANAGE')")
    @GetMapping("/monthly")
    public ResponseEntity<GlobalApiResponse<MonthlyResourceUsageResponse>> getMonthlyUsage(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        MonthlyResourceUsageView view = getMonthlyResourceUsageUseCase.getMonthlyUsage(month);
        return ResponseEntity.ok(GlobalApiResponse.ok(
                ResourceUsageResponseCode.MONTHLY_USAGE_RETRIEVED,
                MonthlyResourceUsageResponse.from(view)));
    }
}
