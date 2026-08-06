package com.academy.mudogroupware.rollcall.application.usecase;

import com.academy.mudogroupware.rollcall.application.command.SaveAttendanceEntriesCommand;

public interface SaveAttendanceEntriesUseCase {

    void saveEntries(SaveAttendanceEntriesCommand command);
}
