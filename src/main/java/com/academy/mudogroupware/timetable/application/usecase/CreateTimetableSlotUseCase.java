package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.command.CreateTimetableSlotCommand;

public interface CreateTimetableSlotUseCase {

    Long createSlot(CreateTimetableSlotCommand command);
}
