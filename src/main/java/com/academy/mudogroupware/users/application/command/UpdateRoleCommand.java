package com.academy.mudogroupware.users.application.command;

public record UpdateRoleCommand(Long roleId, Long academyId, String name, String description, String color) {
}
