package com.academy.mudogroupware.revenuereport.presentation.api.request;

import java.time.LocalDate;
import java.time.YearMonth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GenerateRevenueReportRequest(
        @Schema(description = "생성할 대상 월 (yyyy-MM)", example = "2026-07")
        @NotBlank
        @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "yyyy-MM 형식이어야 합니다")
        String targetMonth) {

    public LocalDate toTargetMonthDate() {
        return YearMonth.parse(targetMonth).atDay(1);
    }
}
