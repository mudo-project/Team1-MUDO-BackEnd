package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.application.query.RoleView;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class GetRoleServiceTest {

    @Test
    void returnsRoleViewWithMemberCountWhenFound() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Role role = Role.restore(1L, "강사", "설명", LocalDateTime.now(), Set.of("NOTICE:READ"));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.countActiveByRoleIds(Set.of(1L))).thenReturn(Map.of(1L, 2L));
        GetRoleService service = new GetRoleService(roleRepository, userRepository);

        RoleView result = service.getRole(1L);

        assertThat(result).isEqualTo(new RoleView(role, 2L));
    }

    @Test
    void returnsZeroMemberCountWhenNoOneAssigned() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Role role = Role.restore(1L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.countActiveByRoleIds(Set.of(1L))).thenReturn(Map.of());
        GetRoleService service = new GetRoleService(roleRepository, userRepository);

        RoleView result = service.getRole(1L);

        assertThat(result).isEqualTo(new RoleView(role, 0L));
    }

    @Test
    void throwsWhenRoleDoesNotExist() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());
        GetRoleService service = new GetRoleService(roleRepository, userRepository);

        assertThatThrownBy(() -> service.getRole(1L))
                .isInstanceOf(RoleNotFoundException.class);
    }
}
