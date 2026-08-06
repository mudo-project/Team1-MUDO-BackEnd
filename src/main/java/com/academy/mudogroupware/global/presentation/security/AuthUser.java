package com.academy.mudogroupware.global.presentation.security;

import java.security.Principal;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;

public record AuthUser(Long userId, String username, Long academyId, Long roleId, String roleName,
                        AccountType accountType, AdminScope adminScope) implements Principal {

    public AuthUser(Long userId, String username, Long academyId, Long roleId, String roleName) {
        this(userId, username, academyId, roleId, roleName, AccountType.MEMBER, null);
    }

    @Override
    public String getName() {
        return username;
    }
}
