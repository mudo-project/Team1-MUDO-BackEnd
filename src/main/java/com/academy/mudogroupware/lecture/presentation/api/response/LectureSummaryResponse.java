package com.academy.mudogroupware.lecture.presentation.api.response;

import java.util.List;

import com.academy.mudogroupware.lecture.application.query.LectureSummaryView;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record LectureSummaryResponse(
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
        List<ScheduleResponse> schedules,
        int studentCount
) {

    public static LectureSummaryResponse from(LectureSummaryView view) {
        List<ScheduleResponse> schedules = view.schedules().stream().map(ScheduleResponse::from).toList();
        return new LectureSummaryResponse(view.id(), view.name(), view.classType(), view.grade(), view.termName(),
                view.subjectName(), view.teacherId(), view.teacherName(), view.classroomCode(),
                view.classroomName(), schedules, view.studentCount());
    }
}
