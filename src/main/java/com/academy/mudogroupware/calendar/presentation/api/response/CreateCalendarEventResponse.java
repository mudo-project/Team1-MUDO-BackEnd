package com.academy.mudogroupware.calendar.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateCalendarEventResponse(
        @Schema(description = "생성된 일정 번호", example = "1")
        Long eventId
) {

    public static CreateCalendarEventResponse from(Long eventId) {
        return new CreateCalendarEventResponse(eventId);
    }
}
