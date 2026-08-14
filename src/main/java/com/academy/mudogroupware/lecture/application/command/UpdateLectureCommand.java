package com.academy.mudogroupware.lecture.application.command;

import java.util.List;

import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record UpdateLectureCommand(
        Long lectureId,
        String name,
        ClassType classType,
        String classroomCode,
        Grade grade,
        String teacherName,
        String subjectName,
        String termName,
        FeeType feeType,
        Integer feeAmount,
        List<ScheduleInput> schedules,
        Long requesterId,
        Long teacherId
) {
}
