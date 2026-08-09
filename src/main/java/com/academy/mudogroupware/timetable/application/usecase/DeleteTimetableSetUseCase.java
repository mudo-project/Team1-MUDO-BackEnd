package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSetCommand;

public interface DeleteTimetableSetUseCase {

    void deleteTimetableSet(DeleteTimetableSetCommand command);
}
