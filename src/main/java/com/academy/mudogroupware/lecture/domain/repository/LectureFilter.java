package com.academy.mudogroupware.lecture.domain.repository;

import java.time.DayOfWeek;

import com.academy.mudogroupware.lecture.domain.model.Grade;

public record LectureFilter(
        Long termId,
        Grade grade,
        String subjectName,
        String teacherName,
        String classroomCode,
        DayOfWeek dayOfWeek
) {
    public LectureFilter {
        subjectName = normalize(subjectName);
        teacherName = normalize(teacherName);
        classroomCode = normalize(classroomCode);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
