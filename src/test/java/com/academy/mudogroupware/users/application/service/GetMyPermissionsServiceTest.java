package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.global.domain.auth.EffectivePermissionResolver;

class GetMyPermissionsServiceTest {

    private final EffectivePermissionResolver effectivePermissionResolver = mock(EffectivePermissionResolver.class);
    private final GetMyPermissionsService service = new GetMyPermissionsService(effectivePermissionResolver);

    @Test
    void returnsResolverResultForMemberWithRole() {
        when(effectivePermissionResolver.resolve(5L, AccountType.MEMBER, null))
                .thenReturn(List.of("ATTENDANCE:CHECK_IN", "STUDENT:MANAGE"));

        List<String> result = service.getMyPermissions(5L, AccountType.MEMBER, null);

        assertThat(result).containsExactly("ATTENDANCE:CHECK_IN", "STUDENT:MANAGE");
    }

    @Test
    void returnsResolverResultForPlatformAdminWithNullRoleId() {
        when(effectivePermissionResolver.resolve(null, AccountType.ADMIN, AdminScope.PLATFORM))
                .thenReturn(List.of("ACCOUNT:MANAGE", "ROLE:MANAGE", "PLATFORM:SUPER_ADMIN"));

        List<String> result = service.getMyPermissions(null, AccountType.ADMIN, AdminScope.PLATFORM);

        assertThat(result).containsExactly("ACCOUNT:MANAGE", "ROLE:MANAGE", "PLATFORM:SUPER_ADMIN");
    }
}
