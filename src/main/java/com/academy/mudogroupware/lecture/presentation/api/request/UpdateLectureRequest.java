package com.academy.mudogroupware.lecture.presentation.api.request;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.application.command.UpdateLectureCommand;
import com.academy.mudogroupware.lecture.domain.model.ClassType;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateLectureRequest(
        @NotBlank String name,
        @NotNull ClassType classType,
        @NotNull DayOfWeek dayOfWeek,
        @NotBlank String classroomCode,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Grade grade,
        String teacherName,
        String subjectName,
        String termName,
        FeeType feeType,
        Integer feeAmount
) {

    public UpdateLectureCommand toCommand(Long lectureId, Long requesterId) {
        List<ScheduleInput> inputs = List.of(new ScheduleInput(dayOfWeek, startTime, endTime));
        return new UpdateLectureCommand(lectureId, name, classType, classroomCode, grade, teacherName,
                subjectName, termName, feeType, feeAmount, inputs, requesterId, null);
    }
}
