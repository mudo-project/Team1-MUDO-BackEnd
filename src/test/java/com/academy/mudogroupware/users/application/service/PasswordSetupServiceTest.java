package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.application.command.PasswordSetupCommand;
import com.academy.mudogroupware.users.domain.exception.PasswordSetupFailedException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class PasswordSetupServiceTest {

    private User pendingUser(String hash) {
        return User.restore(1L, "teacher01", hash, "김강사", "010-1111-2222", "teacher01@example.com", null,
                UserStatus.ACTIVE, true, AccountType.MEMBER, null, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now());
    }

    @Test
    void throwsWhenUserNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        PasswordSetupService service = new PasswordSetupService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.setup(
                new PasswordSetupCommand(1L, "newPassword1!", "new@example.com", "010-1234-5678")))
                .isInstanceOf(PasswordSetupFailedException.class);

        verify(userRepository, never()).completePasswordSetup(any(), any(), any(), any());
    }

    @Test
    void throwsWhenPasswordAlreadySetUp() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User alreadySetUp = User.restore(1L, "teacher01", "hash", "김강사", "010-1111-2222",
                "teacher01@example.com", null, UserStatus.ACTIVE, false, AccountType.MEMBER, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(alreadySetUp));
        PasswordSetupService service = new PasswordSetupService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.setup(
                new PasswordSetupCommand(1L, "newPassword1!", "new@example.com", "010-1234-5678")))
                .isInstanceOf(PasswordSetupFailedException.class);

        verify(userRepository, never()).completePasswordSetup(any(), any(), any(), any());
    }

    @Test
    void completesPasswordSetupWhenUserMustChangePassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser("hash")));
        when(passwordEncoder.encode("newPassword1!")).thenReturn("new-hash");
        when(userRepository.completePasswordSetup(1L, "new-hash", "010-1234-5678", "new@example.com"))
                .thenReturn(true);
        PasswordSetupService service = new PasswordSetupService(userRepository, passwordEncoder);

        service.setup(new PasswordSetupCommand(1L, "newPassword1!", "new@example.com", "010-1234-5678"));

        verify(userRepository).completePasswordSetup(1L, "new-hash", "010-1234-5678", "new@example.com");
    }

    @Test
    void throwsWhenCompletePasswordSetupLosesConcurrentRace() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(pendingUser("hash")));
        when(passwordEncoder.encode("newPassword1!")).thenReturn("new-hash");
        when(userRepository.completePasswordSetup(1L, "new-hash", "010-1234-5678", "new@example.com"))
                .thenReturn(false);
        PasswordSetupService service = new PasswordSetupService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.setup(
                new PasswordSetupCommand(1L, "newPassword1!", "new@example.com", "010-1234-5678")))
                .isInstanceOf(PasswordSetupFailedException.class);
    }
}
