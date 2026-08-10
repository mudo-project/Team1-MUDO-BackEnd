package com.academy.mudogroupware.timetable.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTimetableSlotResponse(
        @Schema(description = "생성된 수업 슬롯 번호", example = "1") Long timetableSlotId
) {

    public static CreateTimetableSlotResponse from(Long timetableSlotId) {
        return new CreateTimetableSlotResponse(timetableSlotId);
    }
}
