package com.academy.mudogroupware.calendar.application.command;

import java.time.LocalDateTime;

public record CreateCalendarEventCommand(
        String title,
        String content,
        LocalDateTime eventStartAt,
        LocalDateTime eventEndAt,
        boolean allDay,
        String color,
        Long createdBy
) {
}
