package com.academy.mudogroupware.global.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

class EffectivePermissionResolverTest {

    private final RolePermissionLookupPort rolePermissionLookupPort = mock(RolePermissionLookupPort.class);
    private final PlatformAdminPermissionPort platformAdminPermissionPort = mock(PlatformAdminPermissionPort.class);
    private final EffectivePermissionResolver resolver =
            new EffectivePermissionResolver(rolePermissionLookupPort, platformAdminPermissionPort);

    @Test
    void resolvesRolePermissionCodesForRegularMember() {
        when(rolePermissionLookupPort.lookup(5L))
                .thenReturn(new RolePermissionInfo("조교", Set.of("ATTENDANCE:CHECK_IN", "STUDENT:MANAGE")));

        var permissions = resolver.resolve(5L, AccountType.MEMBER, null);

        assertThat(permissions).containsExactlyInAnyOrder("ATTENDANCE:CHECK_IN", "STUDENT:MANAGE");
    }

    @Test
    void resolvesFullCatalogPlusCompositeAuthorityForPlatformAdmin() {
        when(platformAdminPermissionPort.allPermissionCodes())
                .thenReturn(Set.of("ACCOUNT:MANAGE", "ROLE:MANAGE"));

        var permissions = resolver.resolve(null, AccountType.ADMIN, AdminScope.PLATFORM);

        assertThat(permissions).containsExactlyInAnyOrder("ACCOUNT:MANAGE", "ROLE:MANAGE", "PLATFORM:SUPER_ADMIN");
    }

    @Test
    void resolvesRolePermissionCodesPlusCompositeAuthorityForAcademyOwner() {
        when(rolePermissionLookupPort.lookup(7L))
                .thenReturn(new RolePermissionInfo("원장", Set.of("ACCOUNT:MANAGE")));

        var permissions = resolver.resolve(7L, AccountType.ADMIN, AdminScope.ACADEMY);

        assertThat(permissions).containsExactlyInAnyOrder("ACCOUNT:MANAGE", "ACADEMY:OWNER");
    }

    @Test
    void doesNotCallPlatformAdminPortForRegularMember() {
        when(rolePermissionLookupPort.lookup(5L)).thenReturn(RolePermissionInfo.empty());

        resolver.resolve(5L, AccountType.MEMBER, null);

        org.mockito.Mockito.verifyNoInteractions(platformAdminPermissionPort);
    }
}
