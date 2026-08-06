package com.academy.mudogroupware.lecture.application.query;

import java.util.List;

import com.academy.mudogroupware.lecture.domain.model.Grade;

public record LectureSummaryView(
        Long id,
        String name,
        Grade grade,
        String termName,
        String subjectName,
        Long teacherId,
        String classroomName,
        List<ScheduleView> schedules,
        int studentCount
) {
}
