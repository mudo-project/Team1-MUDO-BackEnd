package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.auth.AccountType;
import com.academy.mudogroupware.users.domain.model.User;
import com.academy.mudogroupware.users.domain.model.UserStatus;
import com.academy.mudogroupware.users.domain.repository.UserRepository;

class SearchUsersServiceTest {

    @Test
    void returnsMatchingUsersWhenKeywordGiven() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = member(1L, "김강사", "kim_teacher01");
        when(userRepository.search("김")).thenReturn(List.of(user));
        SearchUsersService service = new SearchUsersService(userRepository);

        List<User> result = service.search("김");

        assertThat(result).containsExactly(user);
    }

    @Test
    void returnsAllUsersWhenKeywordIsNull() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = member(1L, "김강사", "kim_teacher01");
        when(userRepository.search(null)).thenReturn(List.of(user));
        SearchUsersService service = new SearchUsersService(userRepository);

        List<User> result = service.search(null);

        assertThat(result).containsExactly(user);
    }

    @Test
    void returnsEmptyListWhenNoMatch() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.search("없는이름")).thenReturn(List.of());
        SearchUsersService service = new SearchUsersService(userRepository);

        List<User> result = service.search("없는이름");

        assertThat(result).isEmpty();
    }

    private User member(Long id, String name, String username) {
        return User.restore(id, username, "hashed", name, "010-0000-0000",
                username + "@example.com", 3L, UserStatus.ACTIVE, false, AccountType.MEMBER, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }
}
