package com.academy.mudogroupware.lecture.presentation.api.response;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.lecture.application.query.LectureDetailView;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record LectureDetailResponse(
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
        FeeType feeType,
        Integer feeAmount,
        List<ScheduleResponse> schedules,
        List<StudentSummaryResponse> students,
        LocalDateTime createdAt
) {

    public static LectureDetailResponse from(LectureDetailView view) {
        List<ScheduleResponse> schedules = view.schedules().stream().map(ScheduleResponse::from).toList();
        List<StudentSummaryResponse> students = view.students().stream().map(StudentSummaryResponse::from).toList();
        return new LectureDetailResponse(view.id(), view.name(), view.classType(), view.grade(), view.termName(),
                view.subjectName(), view.teacherId(), view.teacherName(), view.classroomCode(),
                view.classroomName(), view.feeType(), view.feeAmount(), schedules, students, view.createdAt());
    }
}
