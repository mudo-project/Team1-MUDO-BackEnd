package com.academy.mudogroupware.users.application.query;

import com.academy.mudogroupware.users.domain.model.Role;

public record RoleView(Role role, long memberCount) {
}
