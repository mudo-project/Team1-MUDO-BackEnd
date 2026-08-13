package com.academy.mudogroupware.users.presentation.api.request;

import com.academy.mudogroupware.users.application.command.PasswordSetupCommand;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordSetupRequest(
        @NotBlank @Size(min = 8, max = 100) String newPassword,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 20) String phone
) {

    public PasswordSetupCommand toCommand(Long userId) {
        return new PasswordSetupCommand(userId, newPassword, email, phone);
    }
}
