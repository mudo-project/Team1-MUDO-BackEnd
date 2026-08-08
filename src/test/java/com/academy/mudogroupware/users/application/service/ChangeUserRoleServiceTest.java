package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.global.domain.auth.AdminScope;
import com.academy.mudogroupware.users.application.command.ChangeUserRoleCommand;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class ChangeUserRoleServiceTest {

    @Test
    void throwsWhenTargetUserDoesNotExist() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository, roleRepository);

        assertThatThrownBy(() -> service.changeRole(new ChangeUserRoleCommand(1L, 10L, 5L)))
                .isInstanceOf(UserException.class);
    }

    @Test
    void throwsWhenTargetUserBelongsToDifferentAcademy() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        User user = member(1L, 20L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository, roleRepository);

        assertThatThrownBy(() -> service.changeRole(new ChangeUserRoleCommand(1L, 10L, 5L)))
                .isInstanceOf(UserException.class);
    }

    @Test
    void throwsWhenTargetIsNotMemberAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        User admin = User.restore(1L, 10L, "academy01", "hashed", "원장", "010-0000-0000",
                "admin@example.com", null, UserStatus.ACTIVE, false, AccountType.ADMIN, AdminScope.ACADEMY,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository, roleRepository);

        assertThatThrownBy(() -> service.changeRole(new ChangeUserRoleCommand(1L, 10L, 5L)))
                .isInstanceOf(UserException.class);
    }

    @Test
    void throwsWhenTargetRoleDoesNotExist() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        User user = member(1L, 10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findById(5L)).thenReturn(Optional.empty());
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository, roleRepository);

        assertThatThrownBy(() -> service.changeRole(new ChangeUserRoleCommand(1L, 10L, 5L)))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void throwsWhenTargetRoleBelongsToDifferentAcademy() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        User user = member(1L, 10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Role role = Role.restore(5L, 20L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository, roleRepository);

        assertThatThrownBy(() -> service.changeRole(new ChangeUserRoleCommand(1L, 10L, 5L)))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void changesRoleWhenValid() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        User user = member(1L, 10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Role role = Role.restore(5L, 10L, "강사", "설명", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        ChangeUserRoleService service = new ChangeUserRoleService(userRepository, roleRepository);

        service.changeRole(new ChangeUserRoleCommand(1L, 10L, 5L));

        verify(userRepository).changeRole(1L, 5L);
    }

    private User member(Long id, Long academyId) {
        return User.restore(id, academyId, "member01", "hashed", "구성원", "010-0000-0000",
                "member@example.com", 3L, UserStatus.ACTIVE, false, AccountType.MEMBER, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }
}
