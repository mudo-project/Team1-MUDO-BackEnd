package com.academy.mudogroupware.users.application.command;

public record CreateAccountCommand(String username, String name, Long roleId) {
}
