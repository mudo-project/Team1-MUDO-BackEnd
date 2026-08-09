package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.command.UpdateTimetableSetCommand;

public interface UpdateTimetableSetUseCase {

    void updateTimetableSet(UpdateTimetableSetCommand command);
}
