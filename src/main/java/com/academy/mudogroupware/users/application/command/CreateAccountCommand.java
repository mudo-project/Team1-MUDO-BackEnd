package com.academy.mudogroupware.users.application.command;

public record CreateAccountCommand(String username, String name, String phone, String email,
                                    Long roleId) {
}
