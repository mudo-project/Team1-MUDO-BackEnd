package com.academy.mudogroupware.timetable.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateTimetableSetResponse(
        @Schema(description = "생성된 시간표 세트 번호", example = "1") Long timetableSetId
) {

    public static CreateTimetableSetResponse from(Long timetableSetId) {
        return new CreateTimetableSetResponse(timetableSetId);
    }
}
