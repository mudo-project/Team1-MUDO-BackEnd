package com.academy.mudogroupware.calendar.application.usecase;

import com.academy.mudogroupware.calendar.application.command.UpdateCalendarEventCommand;

public interface UpdateCalendarEventUseCase {

    void updateEvent(UpdateCalendarEventCommand command);
}
