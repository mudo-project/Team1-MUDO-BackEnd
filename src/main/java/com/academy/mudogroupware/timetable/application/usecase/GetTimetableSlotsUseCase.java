package com.academy.mudogroupware.timetable.application.usecase;

import java.util.List;

import com.academy.mudogroupware.timetable.application.query.TimetableSlotView;

public interface GetTimetableSlotsUseCase {

    List<TimetableSlotView> getSlots(Long academyId, Long timetableSetId);
}
