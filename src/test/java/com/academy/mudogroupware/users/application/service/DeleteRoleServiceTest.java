package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.application.command.DeleteRoleCommand;
import com.academy.mudogroupware.users.domain.exception.RoleInUseException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class DeleteRoleServiceTest {

    @Test
    void throwsWhenRoleDoesNotExist() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());
        DeleteRoleService service = new DeleteRoleService(roleRepository, userRepository);

        assertThatThrownBy(() -> service.deleteRole(new DeleteRoleCommand(1L, 10L)))
                .isInstanceOf(RoleNotFoundException.class);

        verify(roleRepository, never()).deleteById(any());
    }

    @Test
    void throwsWhenRoleBelongsToDifferentAcademy() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Role role = Role.restore(1L, 20L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        DeleteRoleService service = new DeleteRoleService(roleRepository, userRepository);

        assertThatThrownBy(() -> service.deleteRole(new DeleteRoleCommand(1L, 10L)))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void throwsWhenRoleIsInUseByActiveMember() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Role role = Role.restore(1L, 10L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.existsActiveByRoleId(1L)).thenReturn(true);
        DeleteRoleService service = new DeleteRoleService(roleRepository, userRepository);

        assertThatThrownBy(() -> service.deleteRole(new DeleteRoleCommand(1L, 10L)))
                .isInstanceOf(RoleInUseException.class);

        verify(userRepository, never()).clearRoleId(any());
        verify(roleRepository, never()).deleteById(any());
    }

    @Test
    void deletesRoleAndClearsInactiveHoldersWhenNoActiveMember() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Role role = Role.restore(1L, 10L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(userRepository.existsActiveByRoleId(1L)).thenReturn(false);
        DeleteRoleService service = new DeleteRoleService(roleRepository, userRepository);

        service.deleteRole(new DeleteRoleCommand(1L, 10L));

        verify(userRepository).clearRoleId(1L);
        verify(roleRepository).deleteById(1L);
    }
}
