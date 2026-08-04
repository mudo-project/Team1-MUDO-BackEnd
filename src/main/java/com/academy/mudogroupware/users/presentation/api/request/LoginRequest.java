package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.LoginCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 100) String password
) {

    public LoginCommand toCommand() {
        return new LoginCommand(username, password);
    }
}
