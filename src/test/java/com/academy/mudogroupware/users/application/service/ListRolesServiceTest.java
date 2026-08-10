package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.users.application.query.RoleView;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class ListRolesServiceTest {

    @Test
    void returnsAllRolesWithMemberCountForAcademy() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Role role = Role.restore(1L, 10L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findAllByAcademyId(10L)).thenReturn(List.of(role));
        when(userRepository.countActiveByRoleIds(Set.of(1L))).thenReturn(Map.of(1L, 4L));
        ListRolesService service = new ListRolesService(roleRepository, userRepository);

        List<RoleView> result = service.listRoles(10L);

        assertThat(result).containsExactly(new RoleView(role, 4L));
    }

    @Test
    void returnsZeroMemberCountWhenNoOneAssigned() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        Role role = Role.restore(1L, 10L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findAllByAcademyId(10L)).thenReturn(List.of(role));
        when(userRepository.countActiveByRoleIds(Set.of(1L))).thenReturn(Map.of());
        ListRolesService service = new ListRolesService(roleRepository, userRepository);

        List<RoleView> result = service.listRoles(10L);

        assertThat(result).containsExactly(new RoleView(role, 0L));
    }

    @Test
    void returnsEmptyListWhenAcademyHasNoRoles() {
        RoleRepository roleRepository = mock(RoleRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(roleRepository.findAllByAcademyId(10L)).thenReturn(List.of());
        ListRolesService service = new ListRolesService(roleRepository, userRepository);

        List<RoleView> result = service.listRoles(10L);

        assertThat(result).isEmpty();
    }
}
