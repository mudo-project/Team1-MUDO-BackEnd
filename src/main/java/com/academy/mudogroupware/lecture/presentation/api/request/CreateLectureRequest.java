package com.academy.mudogroupware.lecture.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.lecture.application.command.CreateLectureCommand;
import com.academy.mudogroupware.lecture.application.command.ScheduleInput;
import com.academy.mudogroupware.lecture.domain.model.FeeType;
import com.academy.mudogroupware.lecture.domain.model.Grade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateLectureRequest(
        @NotBlank String name,
        @NotNull Grade grade,
        @NotBlank String termName,
        @NotBlank String subjectName,
        @NotNull Long teacherId,
        @NotBlank String classroomName,
        FeeType feeType,
        Integer feeAmount,
        @NotEmpty List<@Valid ScheduleRequest> schedules
) {

    public CreateLectureCommand toCommand(Long academyId, Long requesterId) {
        List<ScheduleInput> inputs = schedules.stream().map(ScheduleRequest::toInput).toList();
        return new CreateLectureCommand(academyId, name, grade, termName, subjectName, teacherId, classroomName,
                feeType, feeAmount, inputs, requesterId);
    }
}
