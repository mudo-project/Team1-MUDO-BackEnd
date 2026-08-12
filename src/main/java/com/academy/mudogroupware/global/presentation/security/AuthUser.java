package com.academy.mudogroupware.global.presentation.security;

import java.security.Principal;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;

public record AuthUser(Long userId, String username, Long roleId, String roleName,
                        AccountType accountType, AdminScope adminScope, boolean mustChangePw) implements Principal {

    public AuthUser(Long userId, String username, Long roleId, String roleName) {
        this(userId, username, roleId, roleName, AccountType.MEMBER, null, false);
    }

    @Override
    public String getName() {
        return username;
    }
}
