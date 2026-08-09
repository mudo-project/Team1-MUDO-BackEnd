package com.academy.mudogroupware.timetable.application.command;

import java.util.Map;

import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;

public record ExportTimetableCommand(
        Long academyId, Long timetableSetId, TimetableExportFormat format, Map<ClassType, String> colorHexByClassType) {
}
