package com.academy.mudogroupware.lecture.application.query;

import java.time.LocalDateTime;
import java.util.List;

import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record LectureDetailView(
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
        List<ScheduleView> schedules,
        List<StudentSummaryView> students,
        LocalDateTime createdAt
) {
}
