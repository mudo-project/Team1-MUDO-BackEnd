package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;

public interface GetTimetableSlotUseCase {

    TimetableSlotView getSlot(Long academyId, Long timetableSetId, Long timetableSlotId);
}
