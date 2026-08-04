package com.academy.mudogroupware.global.domain.auth;

public interface RolePermissionLookupPort {

    RolePermissionInfo lookup(Long roleId);
}
