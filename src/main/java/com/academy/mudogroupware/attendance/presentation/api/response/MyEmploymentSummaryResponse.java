package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;

import com.academy.mudogroupware.attendance.application.query.MyEmploymentSummaryView;

import io.swagger.v3.oas.annotations.media.Schema;

public record MyEmploymentSummaryResponse(
        @Schema(example = "2025-06-17") LocalDate hireDate,
        @Schema(example = "416") long tenureDays) {

    public static MyEmploymentSummaryResponse from(MyEmploymentSummaryView view) {
        return new MyEmploymentSummaryResponse(view.hireDate(), view.tenureDays());
    }
}
