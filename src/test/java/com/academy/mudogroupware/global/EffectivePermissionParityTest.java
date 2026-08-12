package com.academy.mudogroupware.global;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.global.domain.auth.EffectivePermissionResolver;
import com.academy.mudogroupware.global.domain.auth.JwtClaims;
import com.academy.mudogroupware.global.domain.auth.RolePermissionInfo;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;

/**
 * EffectivePermissionResolver(로그인 응답용)와 JwtAuthenticationConverter(매 요청 인가용)는
 * 같은 권한 계산 규칙을 각자 구현하고 있다. 이 테스트는 같은 fixture로 두 결과를 직접 비교해서,
 * 규칙이 갈라지면(한쪽만 고치고 다른 쪽을 잊으면) 바로 잡아낸다.
 */
class EffectivePermissionParityTest {

    @Test
    void matchesForRegularMember() {
        RolePermissionInfo info = new RolePermissionInfo("TEACHER", Set.of("WORKSPACE:READ", "NOTICE:WRITE"));
        EffectivePermissionResolver resolver = new EffectivePermissionResolver(
                roleId -> info, () -> {
                    throw new AssertionError("일반 계정은 플랫폼 권한 포트를 호출하면 안 됨");
                });
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(
                roleId -> info, () -> {
                    throw new AssertionError("일반 계정은 플랫폼 권한 포트를 호출하면 안 됨");
                });

        assertParity(resolver, converter, 10L, AccountType.MEMBER, null);
    }

    @Test
    void matchesForPlatformAdmin() {
        Set<String> catalog = Set.of("ROLE:MANAGE", "ACCOUNT:CREATE");
        EffectivePermissionResolver resolver = new EffectivePermissionResolver(
                roleId -> {
                    throw new AssertionError("플랫폼 관리자는 역할 조회를 하면 안 됨");
                }, () -> catalog);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(
                roleId -> {
                    throw new AssertionError("플랫폼 관리자는 역할 조회를 하면 안 됨");
                }, () -> catalog);

        assertParity(resolver, converter, null, AccountType.ADMIN, AdminScope.PLATFORM);
    }

    @Test
    void matchesForAcademyOwner() {
        RolePermissionInfo info = new RolePermissionInfo("학원관리자", Set.of("ACCOUNT:CREATE"));
        EffectivePermissionResolver resolver = new EffectivePermissionResolver(
                roleId -> info, () -> {
                    throw new AssertionError("ACADEMY-scope 관리자는 플랫폼 권한 포트를 쓰지 않음");
                });
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(
                roleId -> info, () -> {
                    throw new AssertionError("ACADEMY-scope 관리자는 플랫폼 권한 포트를 쓰지 않음");
                });

        assertParity(resolver, converter, 5L, AccountType.ADMIN, AdminScope.ACADEMY);
    }

    private void assertParity(EffectivePermissionResolver resolver, JwtAuthenticationConverter converter,
                               Long roleId, AccountType accountType, AdminScope adminScope) {
        List<String> resolverPermissions = resolver.resolve(roleId, accountType, adminScope);

        Authentication authentication = converter.toAuthentication(
                new JwtClaims(1L, "user", roleId, accountType, adminScope, false));
        List<String> converterAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(resolverPermissions).containsExactlyInAnyOrderElementsOf(converterAuthorities);
    }
}
