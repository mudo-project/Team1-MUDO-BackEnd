package com.academy.mudogroupware.lecture.application.command;

import java.util.List;

import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record CreateLectureCommand(
        String name,
        Grade grade,
        String termName,
        String subjectName,
        Long teacherId,
        String classroomName,
        FeeType feeType,
        Integer feeAmount,
        List<ScheduleInput> schedules,
        Long requesterId
) {
}
