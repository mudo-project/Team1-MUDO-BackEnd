package com.academy.mudogroupware.lecture.application.command;

import java.util.List;

import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

public record CreateLectureCommand(
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

    public CreateLectureCommand(String name, Grade grade, String termName, String subjectName, Long teacherId,
                                 String classroomName, FeeType feeType, Integer feeAmount,
                                 List<ScheduleInput> schedules, Long requesterId) {
        this(name, ClassType.CLASS, classroomName, grade, null, subjectName, termName, feeType, feeAmount,
                schedules, requesterId, teacherId);
    }

    public String classroomName() {
        return classroomCode;
    }
}
