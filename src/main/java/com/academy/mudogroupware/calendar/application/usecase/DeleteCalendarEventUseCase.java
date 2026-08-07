package com.academy.mudogroupware.calendar.application.usecase;

import com.academy.mudogroupware.calendar.application.command.DeleteCalendarEventCommand;

public interface DeleteCalendarEventUseCase {

    void deleteEvent(DeleteCalendarEventCommand command);
}
