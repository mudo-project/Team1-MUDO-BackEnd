package com.academy.mudogroupware.timetable.application.query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;
import com.academy.mudogroupware.timetable.domain.model.TimetableSetStatus;

public record TimetableSetDetailView(
        Long timetableSetId, String name, LocalDate startDate, LocalDate endDate,
        LocalTime operatingStartTime, LocalTime operatingEndTime, Set<DayOfWeek> operatingDays,
        int slotUnitMinutes, List<TimetableClassroom> classrooms, TimetableSetStatus status) {
}
