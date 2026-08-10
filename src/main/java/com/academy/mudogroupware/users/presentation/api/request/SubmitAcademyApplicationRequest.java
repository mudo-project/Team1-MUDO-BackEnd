package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.SubmitAcademyApplicationCommand;
import com.academy.mudogroupware.users.domain.model.Plan;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitAcademyApplicationRequest(
        @NotBlank @Size(max = 50) String requestedLoginId,
        @NotBlank @Size(max = 100) String academyName,
        @NotBlank @Size(max = 50) String representativeName,
        @NotBlank @Email @Size(max = 100) String representativeEmail,
        @NotBlank @Size(max = 20) String representativePhone,
        @NotNull Plan plan) {

    public SubmitAcademyApplicationCommand toCommand() {
        return new SubmitAcademyApplicationCommand(
                requestedLoginId, academyName, representativeName, representativeEmail, representativePhone, plan);
    }
}
