package com.academy.mudogroupware.student.presentation.api.request;

import com.academy.mudogroupware.student.application.command.EnrollStudentCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EnrollStudentRequest(
        @Schema(description = "수강 등록할 강의 ID", example = "100")
        @NotNull
        @Positive
        Long lectureId
) {

    public EnrollStudentCommand toCommand(Long academyId, Long studentId) {
        return new EnrollStudentCommand(academyId, studentId, lectureId);
    }
}
