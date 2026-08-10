package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.application.command.CreateRoleCommand;
import com.academy.mudogroupware.users.domain.exception.RoleNameDuplicateException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

class CreateRoleServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-05T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void throwsWhenNameAlreadyExistsInAcademy() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(roleRepository.existsByAcademyIdAndName(1L, "강사")).thenReturn(true);
        CreateRoleService service = new CreateRoleService(roleRepository, clock);

        assertThatThrownBy(() -> service.createRole(new CreateRoleCommand(1L, "강사", "설명", "#FFFFFF")))
                .isInstanceOf(RoleNameDuplicateException.class);

        verify(roleRepository, never()).save(any());
    }

    @Test
    void createsRoleWhenNameIsUnique() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(roleRepository.existsByAcademyIdAndName(1L, "강사")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            assertThat(role.getColor()).isEqualTo("#FFFFFF");
            return Role.restore(10L, role.getAcademyId(), role.getName(), role.getDescription(), role.getColor(),
                    role.getCreatedAt(), role.getPermissionCodes());
        });
        CreateRoleService service = new CreateRoleService(roleRepository, clock);

        Long roleId = service.createRole(new CreateRoleCommand(1L, "강사", "설명", "#FFFFFF"));

        assertThat(roleId).isEqualTo(10L);
    }

    @Test
    void createsRoleWithNullColorWhenNotProvided() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(roleRepository.existsByAcademyIdAndName(1L, "강사")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            assertThat(role.getColor()).isNull();
            return Role.restore(10L, role.getAcademyId(), role.getName(), role.getDescription(), role.getColor(),
                    role.getCreatedAt(), role.getPermissionCodes());
        });
        CreateRoleService service = new CreateRoleService(roleRepository, clock);

        service.createRole(new CreateRoleCommand(1L, "강사", "설명", null));

        verify(roleRepository).save(any());
    }
}
