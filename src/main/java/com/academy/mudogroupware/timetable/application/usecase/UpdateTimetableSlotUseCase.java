package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.command.UpdateTimetableSlotCommand;

public interface UpdateTimetableSlotUseCase {

    void updateSlot(UpdateTimetableSlotCommand command);
}
