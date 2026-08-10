package com.academy.mudogroupware.users.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AcademyApplicationSubmitResponse(
        @Schema(description = "생성된 신청서 ID", example = "1") Long applicationId) {

    public static AcademyApplicationSubmitResponse from(Long applicationId) {
        return new AcademyApplicationSubmitResponse(applicationId);
    }
}
