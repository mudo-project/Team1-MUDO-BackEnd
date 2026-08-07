package com.academy.mudogroupware.calendar.application.usecase;

import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;

public interface GetCalendarEventUseCase {

    CalendarEvent getEvent(Long academyId, Long eventId);
}
