package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.google.application.port.GoogleConnectionUserInfo;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class GoogleConnectionUserDirectoryAdapterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final GoogleConnectionUserDirectoryAdapter adapter = new GoogleConnectionUserDirectoryAdapter(userRepository);

    @Test
    void returnsConnectorNameByUserId() {
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, "김원장")));

        assertThat(adapter.findByUserId(9L))
                .contains(new GoogleConnectionUserInfo(9L, "김원장"));
    }

    @Test
    void returnsEmptyWhenHistoricalConnectorDoesNotExist() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThat(adapter.findByUserId(9L)).isEmpty();
    }

    private User user(Long id, String name) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 15, 12, 0);
        return User.restore(id, "user" + id, "pw", name, "010", "user" + id + "@example.com",
                1L, UserStatus.ACTIVE, false, AccountType.MEMBER, null, now, now, now);
    }
}
