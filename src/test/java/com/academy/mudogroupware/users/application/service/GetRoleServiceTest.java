package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

class GetRoleServiceTest {

    @Test
    void returnsRoleWhenFoundInSameAcademy() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        Role role = Role.restore(1L, 10L, "강사", "설명", LocalDateTime.now(), Set.of("NOTICE:READ"));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        GetRoleService service = new GetRoleService(roleRepository);

        Role result = service.getRole(1L, 10L);

        assertThat(result).isEqualTo(role);
    }

    @Test
    void throwsWhenRoleDoesNotExist() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());
        GetRoleService service = new GetRoleService(roleRepository);

        assertThatThrownBy(() -> service.getRole(1L, 10L))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void throwsWhenRoleBelongsToDifferentAcademy() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        Role role = Role.restore(1L, 20L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        GetRoleService service = new GetRoleService(roleRepository);

        assertThatThrownBy(() -> service.getRole(1L, 10L))
                .isInstanceOf(RoleNotFoundException.class);
    }
}
