package com.academy.mudogroupware.users.application.command;

public record LoginCommand(String username, String password) {

    @Override
    public String toString() {
        return "LoginCommand[username=%s, password=****]".formatted(username);
    }
}
