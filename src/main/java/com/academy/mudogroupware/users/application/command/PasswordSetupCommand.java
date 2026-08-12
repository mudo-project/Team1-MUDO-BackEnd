package com.academy.mudogroupware.users.application.command;

public record PasswordSetupCommand(Long userId, String newPassword, String email, String phone) {
}
