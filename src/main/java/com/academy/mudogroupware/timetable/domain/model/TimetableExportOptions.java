package com.academy.mudogroupware.timetable.domain.model;

import java.util.Map;

public record TimetableExportOptions(
        TimetableExportColorCriterion colorCriterion,
        Map<String, TimetableExportColor> colorsByGroupValue,
        TimetableExportDensity density) {

    private static final TimetableExportColor DEFAULT_COLOR = new TimetableExportColor(255, 255, 255);

    public TimetableExportColor colorFor(String classroomCode, String teacherName) {
        String key = switch (colorCriterion) {
            case CLASSROOM -> classroomCode;
            case TEACHER -> teacherName;
        };
        return key == null ? DEFAULT_COLOR : colorsByGroupValue.getOrDefault(key, DEFAULT_COLOR);
    }
}
