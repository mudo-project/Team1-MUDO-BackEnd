package com.academy.mudogroupware.timetable.application.query;

import java.time.LocalDate;

import com.academy.mudogroupware.timetable.domain.model.TimetableSetStatus;

public record TimetableSetSummaryView(
        Long timetableSetId, String name, LocalDate startDate, LocalDate endDate, TimetableSetStatus status) {
}
