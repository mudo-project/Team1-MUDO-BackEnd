package com.academy.mudogroupware.lecture.domain.repository;

import java.time.DayOfWeek;

import com.academy.mudogroupware.lecture.domain.model.Grade;

public record LectureFilter(
        Long termId,
        Grade grade,
        Long subjectId,
        Long teacherId,
        Long classroomId,
        DayOfWeek dayOfWeek
) {
}
