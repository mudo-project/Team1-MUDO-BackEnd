package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;

class ListRolesServiceTest {

    @Test
    void returnsAllRolesForAcademy() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        Role role = Role.restore(1L, 10L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findAllByAcademyId(10L)).thenReturn(List.of(role));
        ListRolesService service = new ListRolesService(roleRepository);

        List<Role> result = service.listRoles(10L);

        assertThat(result).containsExactly(role);
    }

    @Test
    void returnsEmptyListWhenAcademyHasNoRoles() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(roleRepository.findAllByAcademyId(10L)).thenReturn(List.of());
        ListRolesService service = new ListRolesService(roleRepository);

        List<Role> result = service.listRoles(10L);

        assertThat(result).isEmpty();
    }
}
