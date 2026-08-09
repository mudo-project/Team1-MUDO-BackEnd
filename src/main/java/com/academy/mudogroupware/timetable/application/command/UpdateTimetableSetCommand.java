package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.academy.mudogroupware.timetable.domain.model.TimetableClassroom;

public record UpdateTimetableSetCommand(
        Long academyId, Long timetableSetId, String name, LocalDate startDate, LocalDate endDate,
        LocalTime operatingStartTime, LocalTime operatingEndTime, Set<DayOfWeek> operatingDays,
        int slotUnitMinutes, List<TimetableClassroom> classrooms) {
}
