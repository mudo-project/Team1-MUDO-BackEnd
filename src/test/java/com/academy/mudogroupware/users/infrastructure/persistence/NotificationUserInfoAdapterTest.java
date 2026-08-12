package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.notification.application.query.NotificationUserInfo;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationUserInfoAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 9, 0);

    @Mock
    private UserRepository userRepository;

    @Test
    void returnsEmptyListWhenNoUserIdsRequested() {
        NotificationUserInfoAdapter adapter = new NotificationUserInfoAdapter(userRepository);

        List<NotificationUserInfo> result = adapter.findUserInfo(Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    void returnsNameForEachRequestedUserId() {
        User user = User.restore(10L, "yoonyejin", "encoded-pw", "윤예진", "010-0000-0000",
                "yoonyejin@example.com", 3L, UserStatus.ACTIVE, false, AccountType.MEMBER, null, NOW, NOW, NOW);
        when(userRepository.findAllById(Set.of(10L))).thenReturn(List.of(user));
        NotificationUserInfoAdapter adapter = new NotificationUserInfoAdapter(userRepository);

        List<NotificationUserInfo> result = adapter.findUserInfo(Set.of(10L));

        assertThat(result).containsExactly(new NotificationUserInfo(10L, "윤예진"));
    }
}
