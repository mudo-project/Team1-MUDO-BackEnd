package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.command.ExportTimetableCommand;

public interface ExportTimetableUseCase {

    byte[] export(ExportTimetableCommand command);
}
