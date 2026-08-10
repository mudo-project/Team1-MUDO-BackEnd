package com.academy.mudogroupware.users.application.command;

public record ChangeUserRoleCommand(Long userId, Long academyId, Long roleId) {
}
