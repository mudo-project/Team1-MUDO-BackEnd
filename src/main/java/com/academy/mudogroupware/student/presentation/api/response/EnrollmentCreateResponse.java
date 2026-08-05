package com.academy.mudogroupware.student.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EnrollmentCreateResponse(
        @Schema(description = "생성된 수강 등록 ID", example = "1")
        Long enrollmentId
) {

    public static EnrollmentCreateResponse from(Long enrollmentId) {
        return new EnrollmentCreateResponse(enrollmentId);
    }
}
