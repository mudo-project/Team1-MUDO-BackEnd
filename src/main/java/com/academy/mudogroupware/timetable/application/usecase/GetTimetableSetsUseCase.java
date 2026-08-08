package com.academy.mudogroupware.timetable.application.usecase;

import java.util.List;

import com.academy.mudogroupware.timetable.application.query.TimetableSetSummaryView;

public interface GetTimetableSetsUseCase {

    List<TimetableSetSummaryView> getTimetableSets(Long academyId);
}
