package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.RejectAcademyApplicationCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectAcademyApplicationRequest(
        @NotBlank @Size(max = 255) String rejectReason
) {

    public RejectAcademyApplicationCommand toCommand(Long applicationId, Long reviewerId) {
        return new RejectAcademyApplicationCommand(applicationId, reviewerId, rejectReason);
    }
}
