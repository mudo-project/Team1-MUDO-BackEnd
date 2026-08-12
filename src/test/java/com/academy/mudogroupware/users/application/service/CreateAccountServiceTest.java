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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.command.CreateAccountCommand;
import com.academy.mudogroupware.users.application.result.CreateAccountResult;
import com.academy.mudogroupware.users.application.service.support.AccountIssuer;
import com.academy.mudogroupware.users.application.service.support.IssuedAccount;
import com.academy.mudogroupware.users.domain.exception.RoleNotFoundException;
import com.academy.mudogroupware.users.domain.exception.UsernameDuplicateException;
import com.academy.mudogroupware.users.domain.model.Role;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.RoleRepository;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class CreateAccountServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void throwsWhenUsernameAlreadyExists() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        AccountIssuer accountIssuer = mock(AccountIssuer.class);
        when(userRepository.existsByUsername("teacher01")).thenReturn(true);
        CreateAccountService service = new CreateAccountService(userRepository, roleRepository, accountIssuer, clock);

        assertThatThrownBy(() -> service.createAccount(
                new CreateAccountCommand("teacher01", "김강사", 5L)))
                .isInstanceOf(UsernameDuplicateException.class);

        verify(roleRepository, never()).findById(any());
    }

    @Test
    void throwsWhenRoleNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        AccountIssuer accountIssuer = mock(AccountIssuer.class);
        when(userRepository.existsByUsername("teacher01")).thenReturn(false);
        when(roleRepository.findById(5L)).thenReturn(Optional.empty());
        CreateAccountService service = new CreateAccountService(userRepository, roleRepository, accountIssuer, clock);

        assertThatThrownBy(() -> service.createAccount(
                new CreateAccountCommand("teacher01", "김강사", 5L)))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void createsAccountWhenUsernameUniqueAndRoleValid() {
        UserRepository userRepository = mock(UserRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        AccountIssuer accountIssuer = mock(AccountIssuer.class);
        when(userRepository.existsByUsername("teacher01")).thenReturn(false);
        Role role = Role.restore(5L, "강사", "설명", "#FFFFFF", LocalDateTime.now(), Set.of());
        when(roleRepository.findById(5L)).thenReturn(Optional.of(role));
        User savedUser = User.restore(200L, "teacher01", "hashed", "김강사", null,
                null, 5L, UserStatus.ACTIVE, true, AccountType.MEMBER, null,
                LocalDateTime.now(clock), LocalDateTime.now(clock), LocalDateTime.now(clock));
        when(accountIssuer.issue("teacher01", "김강사", 5L,
                AccountType.MEMBER, null, LocalDateTime.now(clock)))
                .thenReturn(new IssuedAccount(savedUser, "tempPass123!"));
        CreateAccountService service = new CreateAccountService(userRepository, roleRepository, accountIssuer, clock);

        CreateAccountResult result = service.createAccount(
                new CreateAccountCommand("teacher01", "김강사", 5L));

        assertThat(result.userId()).isEqualTo(200L);
        assertThat(result.username()).isEqualTo("teacher01");
        assertThat(result.temporaryPassword()).isEqualTo("tempPass123!");
    }
}
