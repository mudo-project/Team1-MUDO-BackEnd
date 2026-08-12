package com.academy.mudogroupware.messenger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.users.application.usecase.UserDirectoryUseCase;

class ChatMemberDirectoryPortAdapterTest {

    private final ChatMemberInfoJpaRepository chatMemberInfoJpaRepository =
            mock(ChatMemberInfoJpaRepository.class);
    private final UserDirectoryUseCase userDirectoryUseCase = mock(UserDirectoryUseCase.class);
    private final ChatMemberDirectoryPortAdapter adapter =
            new ChatMemberDirectoryPortAdapter(chatMemberInfoJpaRepository, userDirectoryUseCase);

    @Test
    void getMembersReturnsOnlyActiveUsers() {
        ChatMemberInfoEntity active = member(1L, "active");
        ChatMemberInfoEntity inactive = member(2L, "inactive");
        when(chatMemberInfoJpaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(active, inactive));
        when(userDirectoryUseCase.findActiveUserIds(null, Set.of(1L, 2L))).thenReturn(Set.of(1L));

        Map<Long, ChatMemberInfo> members = adapter.getMembers(List.of(1L, 2L));

        assertThat(members).containsOnlyKeys(1L);
    }

    private ChatMemberInfoEntity member(Long id, String name) {
        ChatMemberInfoEntity entity = new ChatMemberInfoEntity();
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "name", name);
        return entity;
    }
}
