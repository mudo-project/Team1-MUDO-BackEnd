package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.RejectAcademyApplicationCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAcademyApplicationRequest(
        @Schema(description = "반려 사유", example = "사업자번호 확인 불가")
        @NotBlank @Size(max = 255) String rejectReason
) {

    public RejectAcademyApplicationCommand toCommand(Long applicationId, Long reviewerId) {
        return new RejectAcademyApplicationCommand(applicationId, reviewerId, rejectReason);
    }
}
