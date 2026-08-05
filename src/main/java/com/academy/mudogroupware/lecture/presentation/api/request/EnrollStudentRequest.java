package com.academy.mudogroupware.lecture.presentation.api.request;

import com.academy.mudogroupware.lecture.application.command.EnrollStudentCommand;

import jakarta.validation.constraints.NotNull;

public record EnrollStudentRequest(
        @NotNull Long studentId
) {

    public EnrollStudentCommand toCommand(Long lectureId) {
        return new EnrollStudentCommand(lectureId, studentId);
    }
}
