package com.academy.mudogroupware.calendar.application.command;

import java.time.LocalDateTime;

public record UpdateCalendarEventCommand(
        Long eventId,
        Long academyId,
        String title,
        String content,
        LocalDateTime eventStartAt,
        LocalDateTime eventEndAt,
        boolean allDay,
        String color
) {
}
