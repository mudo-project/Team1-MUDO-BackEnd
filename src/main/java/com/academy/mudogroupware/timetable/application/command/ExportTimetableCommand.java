package com.academy.mudogroupware.timetable.application.command;

import java.time.DayOfWeek;
import java.util.Map;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColorCriterion;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

public record ExportTimetableCommand(
        Long timetableSetId, TimetableExportFormat format,
        TimetableExportColorCriterion colorCriterion, Map<String, String> colorHexByGroupValue,
        TimetableExportDensity density, DayOfWeek dayOfWeek, String floor, ClassType classType) {
}
