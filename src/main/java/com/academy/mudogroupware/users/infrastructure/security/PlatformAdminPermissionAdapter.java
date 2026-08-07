package com.academy.mudogroupware.users.infrastructure.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.global.domain.auth.PlatformAdminPermissionPort;
import com.academy.mudogroupware.users.domain.model.Permission;
import com.academy.mudogroupware.users.domain.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlatformAdminPermissionAdapter implements PlatformAdminPermissionPort {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<String> allPermissionCodes() {
        return permissionRepository.findAll().stream()
                .map(Permission::code)
                .collect(Collectors.toSet());
    }
}
