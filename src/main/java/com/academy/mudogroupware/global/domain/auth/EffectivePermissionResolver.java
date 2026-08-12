package com.academy.mudogroupware.global.domain.auth;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 계정이 최종적으로 갖는 권한 코드 전체를 계산한다. {@code JwtAuthenticationConverter}가 매 요청마다
 * authority를 계산하는 규칙과 반드시 같아야 한다 — 한쪽을 고치면 다른 쪽도 확인할 것.
 */
@Component
@RequiredArgsConstructor
public class EffectivePermissionResolver {

    private static final String PLATFORM_SUPER_ADMIN = "PLATFORM:SUPER_ADMIN";
    private static final String ACADEMY_OWNER = "ACADEMY:OWNER";

    private final RolePermissionLookupPort rolePermissionLookupPort;
    private final PlatformAdminPermissionPort platformAdminPermissionPort;

    public List<String> resolve(Long roleId, AccountType accountType, AdminScope adminScope) {
        boolean isPlatformAdmin = accountType == AccountType.ADMIN && adminScope == AdminScope.PLATFORM;
        boolean isAcademyOwner = accountType == AccountType.ADMIN && adminScope == AdminScope.ACADEMY;

        List<String> permissions = new ArrayList<>(isPlatformAdmin
                ? platformAdminPermissionPort.allPermissionCodes()
                : rolePermissionLookupPort.lookup(roleId).permissionCodes());

        if (isPlatformAdmin) {
            permissions.add(PLATFORM_SUPER_ADMIN);
        }
        if (isAcademyOwner) {
            permissions.add(ACADEMY_OWNER);
        }
        return permissions;
    }
}
