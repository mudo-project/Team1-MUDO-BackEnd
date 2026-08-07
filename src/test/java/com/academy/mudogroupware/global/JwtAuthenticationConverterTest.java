package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.global.domain.auth.JwtClaims;
import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;

class JwtAuthenticationConverterTest {

    @Test
    void platformAdminGetsAllCatalogPermissionsWithoutRoleLookup() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(
                roleId -> {
                    throw new AssertionError("플랫폼 관리자는 역할 조회를 하면 안 됨");
                },
                () -> Set.of("ROLE:MANAGE", "ACCOUNT:CREATE"));

        Authentication authentication = converter.toAuthentication(
                new JwtClaims(1L, "super-admin", null, 99L, AccountType.ADMIN, AdminScope.PLATFORM));

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE:MANAGE", "ACCOUNT:CREATE");
        AuthUser principal = (AuthUser) authentication.getPrincipal();
        assertThat(principal.accountType()).isEqualTo(AccountType.ADMIN);
        assertThat(principal.adminScope()).isEqualTo(AdminScope.PLATFORM);
        assertThat(principal.roleName()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void memberUsesExistingRoleLookup() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(
                roleId -> new RolePermissionInfo("TEACHER", Set.of("WORKSPACE:READ")),
                () -> {
                    throw new AssertionError("일반 사용자는 플랫폼 권한 포트를 호출하면 안 됨");
                });

        Authentication authentication = converter.toAuthentication(
                new JwtClaims(2L, "teacher", 10L, 1L, AccountType.MEMBER, null));

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("WORKSPACE:READ");
        AuthUser principal = (AuthUser) authentication.getPrincipal();
        assertThat(principal.accountType()).isEqualTo(AccountType.MEMBER);
        assertThat(principal.roleName()).isEqualTo("TEACHER");
    }

    @Test
    void academyScopedAdminStillUsesRoleLookupForNow() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(
                roleId -> new RolePermissionInfo("학원관리자", Set.of("ACCOUNT:CREATE")),
                () -> {
                    throw new AssertionError("ACADEMY-scope 관리자는 아직 플랫폼 권한 포트를 쓰지 않음");
                });

        Authentication authentication = converter.toAuthentication(
                new JwtClaims(3L, "academy-admin", 5L, 1L, AccountType.ADMIN, AdminScope.ACADEMY));

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ACCOUNT:CREATE");
    }
}
