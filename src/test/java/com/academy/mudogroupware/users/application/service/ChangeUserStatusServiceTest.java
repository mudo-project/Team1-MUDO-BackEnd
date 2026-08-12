package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.exception.UserException;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class ChangeUserStatusServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ChangeUserStatusService service = new ChangeUserStatusService(userRepository);

    private User user(long id, AccountType accountType, UserStatus status) {
        LocalDateTime now = LocalDateTime.now();
        return User.restore(id, "user" + id, "hash", "이름", "010-0000-0000",
                "user@example.com", 5L, status, false, accountType, null, now, now, now);
    }

    @Test
    void changesStatusFromActiveToResigned() {
        when(userRepository.findById(2L)).thenReturn(
                Optional.of(user(2L, AccountType.MEMBER, UserStatus.ACTIVE)));

        service.changeStatus(2L, UserStatus.RESIGNED);

        verify(userRepository).changeStatus(2L, UserStatus.RESIGNED);
    }

    @Test
    void changesStatusBackFromInactiveToActive() {
        when(userRepository.findById(2L)).thenReturn(
                Optional.of(user(2L, AccountType.MEMBER, UserStatus.INACTIVE)));

        service.changeStatus(2L, UserStatus.ACTIVE);

        verify(userRepository).changeStatus(2L, UserStatus.ACTIVE);
    }

    @Test
    void throwsWhenTargetIsNotMember() {
        when(userRepository.findById(2L)).thenReturn(
                Optional.of(user(2L, AccountType.ADMIN, UserStatus.ACTIVE)));

        assertThatThrownBy(() -> service.changeStatus(2L, UserStatus.RESIGNED))
                .isInstanceOf(UserException.class);
    }
}
