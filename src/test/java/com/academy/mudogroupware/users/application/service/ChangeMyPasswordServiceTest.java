package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class ChangeMyPasswordServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ChangeMyPasswordService service = new ChangeMyPasswordService(userRepository, passwordEncoder);

    private User user(long id) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(id, 1L, "user" + id, "old-hash", "이름", "010-0000-0000",
                "user@example.com", 5L, UserStatus.ACTIVE, false, AccountType.MEMBER, null, now, now, now);
    }

    @Test
    void replacesPasswordWhenCurrentPasswordMatches() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(passwordEncoder.matches("current-pw", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-pw")).thenReturn("new-hash");

        service.changePassword(1L, "current-pw", "new-pw");

        verify(userRepository).changePassword(1L, "new-hash");
    }

    @Test
    void throwsWhenCurrentPasswordDoesNotMatch() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(passwordEncoder.matches("wrong-pw", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(1L, "wrong-pw", "new-pw"))
                .isInstanceOf(UserException.class);
    }
}
