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

import com.academy.mudogroupware.users.application.command.UpdateRoleCommand;
import com.academy.mudogroupware.users.domain.exception.RoleNameDuplicateException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

class UpdateRoleServiceTest {

    @Test
    void throwsWhenRoleDoesNotExist() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());
        UpdateRoleService service = new UpdateRoleService(roleRepository);

        assertThatThrownBy(() -> service.updateRole(new UpdateRoleCommand(1L, "조교", "설명", "#FFFFFF")))
                .isInstanceOf(RoleNotFoundException.class);

        verify(roleRepository, never()).updateNameAndDescription(any(), any(), any(), any());
    }

    @Test
    void throwsWhenNameAlreadyExistsExcludingSelf() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        Role role = Role.restore(1L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByNameAndIdNot("조교", 1L)).thenReturn(true);
        UpdateRoleService service = new UpdateRoleService(roleRepository);

        assertThatThrownBy(() -> service.updateRole(new UpdateRoleCommand(1L, "조교", "설명", "#FFFFFF")))
                .isInstanceOf(RoleNameDuplicateException.class);

        verify(roleRepository, never()).updateNameAndDescription(any(), any(), any(), any());
    }

    @Test
    void updatesNameDescriptionAndColorWhenValid() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        Role role = Role.restore(1L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(roleRepository.existsByNameAndIdNot("조교", 1L)).thenReturn(false);
        UpdateRoleService service = new UpdateRoleService(roleRepository);

        service.updateRole(new UpdateRoleCommand(1L, "조교", "새 설명", "#000000"));

        verify(roleRepository).updateNameAndDescription(1L, "조교", "새 설명", "#000000");
    }
}
