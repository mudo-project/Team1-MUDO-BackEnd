package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.CreateAccountCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 100) String email,
        @NotNull Long roleId
) {

    public CreateAccountCommand toCommand(Long academyId) {
        return new CreateAccountCommand(academyId, username, name, phone, email, roleId);
    }
}
