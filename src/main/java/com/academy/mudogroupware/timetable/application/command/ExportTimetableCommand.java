package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

public record ExportTimetableCommand(
        Long timetableSetId, TimetableExportFormat format, TimetableExportDensity density,
        DayOfWeek dayOfWeek, String floor, ClassType classType) {
}
