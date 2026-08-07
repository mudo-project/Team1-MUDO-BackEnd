package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalDate;

import com.academy.mudogroupware.attendance.application.query.MyLeaveSummaryView;

import io.swagger.v3.oas.annotations.media.Schema;

public record MyLeaveSummaryResponse(
        @Schema(example = "15") int totalDays,
        @Schema(example = "5") int usedDays,
        @Schema(example = "2") int pendingDays,
        @Schema(example = "8") int remainingDays,
        @Schema(example = "2027-03-01", nullable = true) LocalDate nextGrantDate) {

    public static MyLeaveSummaryResponse from(MyLeaveSummaryView view) {
        return new MyLeaveSummaryResponse(
                view.totalDays(), view.usedDays(), view.pendingDays(),
                view.remainingDays(), view.nextGrantDate());
    }
}
