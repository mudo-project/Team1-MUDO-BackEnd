package com.academy.mudogroupware.users.application.command;

public record CreateRoleCommand(Long academyId, String name, String description) {
}
