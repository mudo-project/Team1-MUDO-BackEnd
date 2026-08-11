package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.result.UserDetailResult;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class GetUserDetailServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final GetUserDetailService service = new GetUserDetailService(userRepository, roleRepository);

    private User user(long id, long academyId, Long roleId, AccountType accountType) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(id, academyId, "user" + id, "hash", "이름" + id, "010-0000-0000",
                "user" + id + "@example.com", roleId, UserStatus.ACTIVE, false, accountType, null, now, now, now);
    }

    @Test
    void getMyProfileReturnsOwnDetailWithRoleName() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, 1L, 5L, AccountType.MEMBER)));
        when(roleRepository.findById(5L)).thenReturn(
                Optional.of(Role.restore(5L, 1L, "강사", null, LocalDateTime.now(), Set.of())));

        UserDetailResult result = service.getMyProfile(1L);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.roleName()).isEqualTo("강사");
    }

    @Test
    void getMyProfileReturnsNullRoleNameWhenNoRoleAssigned() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, 1L, null, AccountType.ADMIN)));

        UserDetailResult result = service.getMyProfile(1L);

        assertThat(result.roleName()).isNull();
    }
}
