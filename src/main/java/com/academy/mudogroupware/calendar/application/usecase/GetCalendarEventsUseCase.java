package com.academy.mudogroupware.calendar.application.usecase;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.calendar.domain.model.CalendarEvent;

public interface GetCalendarEventsUseCase {

    List<CalendarEvent> getEvents(LocalDateTime from, LocalDateTime to);
}
