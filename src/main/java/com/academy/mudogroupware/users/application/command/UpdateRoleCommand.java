package com.academy.mudogroupware.users.application.command;

public record UpdateRoleCommand(Long roleId, String name, String description, String color) {
}
