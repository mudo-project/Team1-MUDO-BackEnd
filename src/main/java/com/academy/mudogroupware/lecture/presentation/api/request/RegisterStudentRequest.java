package com.academy.mudogroupware.lecture.presentation.api.request;

import com.academy.mudogroupware.lecture.application.command.RegisterStudentCommand;
import com.academy.mudogroupware.lecture.domain.model.Grade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterStudentRequest(
        @NotBlank String name,
        @NotNull Grade grade,
        String school,
        String phone,
        String parentPhone,
        String note
) {

    public RegisterStudentCommand toCommand(Long academyId) {
        return new RegisterStudentCommand(academyId, name, grade, school, phone, parentPhone, note);
    }
}
