package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.CreateAccountCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 50) String name,
        @Size(max = 20) String phone,
        @Email @Size(max = 100) String email,
        @NotNull Long roleId
) {

    public CreateAccountCommand toCommand() {
        return new CreateAccountCommand(username, name, phone, email, roleId);
    }
}
