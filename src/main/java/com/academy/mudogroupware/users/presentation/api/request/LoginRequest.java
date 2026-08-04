package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.LoginCommand;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {

    public LoginCommand toCommand() {
        return new LoginCommand(username, password);
    }
}
