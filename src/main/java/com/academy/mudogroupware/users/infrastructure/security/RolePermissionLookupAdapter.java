package com.academy.mudogroupware.users.infrastructure.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.domain.auth.RolePermissionLookupPort;
import com.academy.mudogroupware.users.infrastructure.persistence.RoleEntity;
import com.academy.mudogroupware.users.infrastructure.persistence.RoleJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RolePermissionLookupAdapter implements RolePermissionLookupPort {

    private final RoleJpaRepository roleJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public RolePermissionInfo lookup(Long roleId) {
        if (roleId == null) {
            return RolePermissionInfo.empty();
        }

        return roleJpaRepository.findWithPermissionsById(roleId)
                .map(this::toInfo)
                .orElseGet(RolePermissionInfo::empty);
    }

    private RolePermissionInfo toInfo(RoleEntity role) {
        Set<String> codes = role.getPermissions().stream()
                .map(permission -> permission.getCode())
                .collect(Collectors.toSet());
        return new RolePermissionInfo(role.getName(), codes);
    }
}
