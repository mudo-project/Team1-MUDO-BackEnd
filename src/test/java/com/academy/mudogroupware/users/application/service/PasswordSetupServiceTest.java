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
        return User.restore(1L, 1L, "teacher01", hash, "김강사", "010-1111-2222", "teacher01@example.com", null,
                UserStatus.ACTIVE, true, AccountType.MEMBER, null, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now());
    }

    @Test
    void throwsWhenUserNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findByUsername("teacher01")).thenReturn(Optional.empty());
        PasswordSetupService service = new PasswordSetupService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.setup(new PasswordSetupCommand("teacher01", "temp", "newPassword1!")))
                .isInstanceOf(PasswordSetupFailedException.class);

        verify(userRepository, never()).completePasswordSetup(any(), any());
    }

    @Test
    void throwsWhenPasswordAlreadySetUp() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        User alreadySetUp = User.restore(1L, 1L, "teacher01", "hash", "김강사", "010-1111-2222",
                "teacher01@example.com", null, UserStatus.ACTIVE, false, AccountType.MEMBER, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
        when(userRepository.findByUsername("teacher01")).thenReturn(Optional.of(alreadySetUp));
        PasswordSetupService service = new PasswordSetupService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.setup(new PasswordSetupCommand("teacher01", "temp", "newPassword1!")))
                .isInstanceOf(PasswordSetupFailedException.class);

        verify(userRepository, never()).completePasswordSetup(any(), any());
    }

    @Test
    void throwsWhenTempPasswordDoesNotMatch() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findByUsername("teacher01")).thenReturn(Optional.of(pendingUser("hash")));
        when(passwordEncoder.matches("wrong-temp", "hash")).thenReturn(false);
        PasswordSetupService service = new PasswordSetupService(userRepository, passwordEncoder);

        assertThatThrownBy(() -> service.setup(new PasswordSetupCommand("teacher01", "wrong-temp", "newPassword1!")))
                .isInstanceOf(PasswordSetupFailedException.class);

        verify(userRepository, never()).completePasswordSetup(any(), any());
    }

    @Test
    void completesPasswordSetupWhenTempPasswordMatches() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(userRepository.findByUsername("teacher01")).thenReturn(Optional.of(pendingUser("hash")));
        when(passwordEncoder.matches("correct-temp", "hash")).thenReturn(true);
        when(passwordEncoder.encode("newPassword1!")).thenReturn("new-hash");
        PasswordSetupService service = new PasswordSetupService(userRepository, passwordEncoder);

        service.setup(new PasswordSetupCommand("teacher01", "correct-temp", "newPassword1!"));

        verify(userRepository).completePasswordSetup(1L, "new-hash");
    }
}
