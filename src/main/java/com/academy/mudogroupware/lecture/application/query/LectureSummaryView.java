package com.academy.mudogroupware.lecture.application.query;

import java.util.List;

import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record LectureSummaryView(
        Long id,
        String name,
        ClassType classType,
        Grade grade,
        String termName,
        String subjectName,
        Long teacherId,
        String teacherName,
        String classroomCode,
        String classroomName,
        List<ScheduleView> schedules,
        int studentCount
) {
}
