package com.academy.mudogroupware.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.users.domain.repository.UserRepository;
import com.academy.mudogroupware.users.infrastructure.persistence.UserJpaRepository;
import com.academy.mudogroupware.users.infrastructure.persistence.UserRepositoryImpl;

@ExtendWith(MockitoExtension.class)
class UserDirectoryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void returnsOnlyActiveUserIdsInTheRequestedAcademy() {
        Set<Long> requestedUserIds = Set.of(10L, 20L, 30L, 40L);
        when(userRepository.findActiveUserIds(1L, requestedUserIds))
                .thenReturn(Set.of(10L, 40L));

        UserDirectoryService service = new UserDirectoryService(userRepository);

        Set<Long> activeUserIds = service.findActiveUserIds(1L, requestedUserIds);

        assertThat(activeUserIds).containsExactlyInAnyOrder(10L, 40L);
    }

    @Test
    void returnsEmptySetWithoutQueryingJpaForEmptyUserIds() {
        UserJpaRepository userJpaRepository = org.mockito.Mockito.mock(UserJpaRepository.class);
        UserRepositoryImpl repository = new UserRepositoryImpl(userJpaRepository);

        Set<Long> activeUserIds = repository.findActiveUserIds(1L, Set.of());

        assertThat(activeUserIds).isEmpty();
        verifyNoInteractions(userJpaRepository);
    }
}
