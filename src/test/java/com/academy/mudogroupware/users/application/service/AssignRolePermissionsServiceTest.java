package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.application.command.AssignRolePermissionsCommand;
import com.academy.mudogroupware.users.domain.exception.InvalidPermissionCodeException;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.model.Permission;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.PermissionRepository;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

class AssignRolePermissionsServiceTest {

    @Test
    void throwsWhenRoleDoesNotExist() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        when(roleRepository.findById(1L)).thenReturn(Optional.empty());
        AssignRolePermissionsService service =
                new AssignRolePermissionsService(roleRepository, permissionRepository);

        assertThatThrownBy(() -> service.assignPermissions(
                new AssignRolePermissionsCommand(1L, 10L, Set.of("NOTICE:READ"))))
                .isInstanceOf(RoleNotFoundException.class);

        verify(roleRepository, never()).updatePermissions(any(), any());
    }

    @Test
    void throwsWhenRoleBelongsToDifferentAcademy() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        Role role = Role.restore(1L, 20L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        AssignRolePermissionsService service =
                new AssignRolePermissionsService(roleRepository, permissionRepository);

        assertThatThrownBy(() -> service.assignPermissions(
                new AssignRolePermissionsCommand(1L, 10L, Set.of("NOTICE:READ"))))
                .isInstanceOf(RoleNotFoundException.class);

        verify(roleRepository, never()).updatePermissions(any(), any());
    }

    @Test
    void throwsWhenPermissionCodeDoesNotExist() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        Role role = Role.restore(1L, 10L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllByCodeIn(Set.of("NOTICE:READ", "WRONG:CODE")))
                .thenReturn(List.of(new Permission(1L, "NOTICE:READ", "NOTICE", "READ", null)));
        AssignRolePermissionsService service =
                new AssignRolePermissionsService(roleRepository, permissionRepository);

        assertThatThrownBy(() -> service.assignPermissions(
                new AssignRolePermissionsCommand(1L, 10L, Set.of("NOTICE:READ", "WRONG:CODE"))))
                .isInstanceOf(InvalidPermissionCodeException.class);

        verify(roleRepository, never()).updatePermissions(any(), any());
    }

    @Test
    void assignsPermissionsWhenRoleAndCodesAreValid() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        PermissionRepository permissionRepository = mock(PermissionRepository.class);
        Role role = Role.restore(1L, 10L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllByCodeIn(Set.of("NOTICE:READ")))
                .thenReturn(List.of(new Permission(1L, "NOTICE:READ", "NOTICE", "READ", null)));
        AssignRolePermissionsService service =
                new AssignRolePermissionsService(roleRepository, permissionRepository);

        service.assignPermissions(new AssignRolePermissionsCommand(1L, 10L, Set.of("NOTICE:READ")));

        verify(roleRepository).updatePermissions(1L, Set.of("NOTICE:READ"));
    }
}
