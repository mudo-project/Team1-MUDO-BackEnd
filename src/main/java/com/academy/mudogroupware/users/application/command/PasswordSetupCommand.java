package com.academy.mudogroupware.users.application.command;

public record PasswordSetupCommand(String username, String tempPassword, String newPassword) {
}
