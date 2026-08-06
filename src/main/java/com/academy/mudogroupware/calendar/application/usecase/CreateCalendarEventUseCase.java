package com.academy.mudogroupware.calendar.application.usecase;

import com.academy.mudogroupware.calendar.application.command.CreateCalendarEventCommand;

public interface CreateCalendarEventUseCase {

    Long createEvent(CreateCalendarEventCommand command);
}
