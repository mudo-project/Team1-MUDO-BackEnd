package com.academy.mudogroupware.users.application.command;

import java.util.Set;

public record AssignRolePermissionsCommand(Long roleId, Long academyId, Set<String> permissionCodes) {
}
