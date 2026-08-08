package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSetCommand;

public interface CreateTimetableSetUseCase {

    Long createTimetableSet(CreateTimetableSetCommand command);
}
