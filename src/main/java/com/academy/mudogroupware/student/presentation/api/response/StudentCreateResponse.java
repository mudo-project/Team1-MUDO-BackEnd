package com.academy.mudogroupware.student.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record StudentCreateResponse(
        @Schema(description = "생성된 학생 ID", example = "1")
        Long studentId
) {

    public static StudentCreateResponse from(Long studentId) {
        return new StudentCreateResponse(studentId);
    }
}
