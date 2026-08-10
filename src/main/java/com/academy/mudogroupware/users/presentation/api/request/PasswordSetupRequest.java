package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.PasswordSetupCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordSetupRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(max = 100) String tempPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {

    public PasswordSetupCommand toCommand() {
        return new PasswordSetupCommand(username, tempPassword, newPassword);
    }
}
