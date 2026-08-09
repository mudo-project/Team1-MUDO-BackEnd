package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSlotCommand;

public interface DeleteTimetableSlotUseCase {

    void deleteSlot(DeleteTimetableSlotCommand command);
}
