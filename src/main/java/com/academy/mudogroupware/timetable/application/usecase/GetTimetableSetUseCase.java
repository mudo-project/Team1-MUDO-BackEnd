package com.academy.mudogroupware.timetable.application.usecase;

import com.academy.mudogroupware.timetable.application.query.TimetableSetDetailView;

public interface GetTimetableSetUseCase {

    TimetableSetDetailView getTimetableSet(Long academyId, Long timetableSetId);
}
