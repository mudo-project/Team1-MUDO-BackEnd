package com.academy.mudogroupware.timetable.presentation.api.response;

import java.time.LocalDate;

import com.academy.mudogroupware.timetable.application.query.TimetableSetSummaryView;
import com.academy.mudogroupware.timetable.domain.model.TimetableSetStatus;

import io.swagger.v3.oas.annotations.media.Schema;

public record TimetableSetSummaryResponse(
        @Schema(description = "시간표 세트 번호", example = "1") Long timetableSetId,
        @Schema(description = "이름", example = "2026 여름특강") String name,
        @Schema(description = "시작일") LocalDate startDate,
        @Schema(description = "종료일") LocalDate endDate,
        @Schema(description = "상태(PLANNED/ACTIVE/ENDED)") TimetableSetStatus status
) {

    public static TimetableSetSummaryResponse from(TimetableSetSummaryView view) {
        return new TimetableSetSummaryResponse(
                view.timetableSetId(), view.name(), view.startDate(), view.endDate(), view.status());
    }
}
