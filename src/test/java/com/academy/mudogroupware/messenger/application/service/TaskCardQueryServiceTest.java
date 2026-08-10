package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.query.TaskCardPageView;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskAssignee;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

class TaskCardQueryServiceTest {

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatTaskCardRepository chatTaskCardRepository = mock(ChatTaskCardRepository.class);
    private final ChatMemberDirectoryPort chatMemberDirectoryPort = mock(ChatMemberDirectoryPort.class);
    private final TaskCardQueryService service =
            new TaskCardQueryService(chatRoomRepository, chatTaskCardRepository, chatMemberDirectoryPort);

    @Test
    void rejectsOversizedPageSizeBeforeQuerying() {
        assertThatThrownBy(() -> service.getTaskCards(1L, 1L, null, null, 101))
                .isInstanceOf(MessengerException.class);

        verifyNoInteractions(chatRoomRepository, chatTaskCardRepository, chatMemberDirectoryPort);
    }

    @Test
    void rejectsIncompleteCursorBeforeQuerying() {
        assertThatThrownBy(() -> service.getTaskCards(1L, 1L, LocalDateTime.now(), null, 20))
                .isInstanceOf(MessengerException.class);

        verifyNoInteractions(chatRoomRepository, chatTaskCardRepository, chatMemberDirectoryPort);
    }

    @Test
    void returnsHasNextTrueWhenMoreCardsExistBeyondPageSize() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 7, 10, 0);
        ChatRoom chatRoom = ChatRoom.restore(1L, "group", ChatRoomType.GROUP, 1L,
                List.of(ChatRoomMember.restore(1L, null)), createdAt.minusHours(1));
        ChatTaskCard first = ChatTaskCard.restore(30L, 1L, 1L, "card30", null,
                List.of(ChatTaskAssignee.restore(1L, null)), createdAt.plusSeconds(2), null);
        ChatTaskCard second = ChatTaskCard.restore(29L, 1L, 1L, "card29", null,
                List.of(ChatTaskAssignee.restore(1L, null)), createdAt.plusSeconds(1), null);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatTaskCardRepository.findPage(1L, null, null, 1)).thenReturn(List.of(first, second));
        when(chatMemberDirectoryPort.getMembers(List.of(1L))).thenReturn(
                Map.of(1L, new ChatMemberInfo(1L, "assigner", 10L)));

        TaskCardPageView view = service.getTaskCards(1L, 1L, null, null, 1);

        assertThat(view.taskCards()).hasSize(1);
        assertThat(view.taskCards().get(0).id()).isEqualTo(30L);
        assertThat(view.hasNext()).isTrue();
        assertThat(view.nextCursorCreatedAt()).isEqualTo(createdAt.plusSeconds(2));
        assertThat(view.nextCursorCardId()).isEqualTo(30L);
    }

    @Test
    void throwsWhenRequesterIsNotRoomMember() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 7, 10, 0);
        ChatRoom chatRoom = ChatRoom.restore(1L, "group", ChatRoomType.GROUP, 1L,
                List.of(ChatRoomMember.restore(1L, null)), createdAt.minusHours(1));
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> service.getTaskCards(1L, 99L, null, null, 20))
                .isInstanceOf(MessengerException.class);

        verifyNoInteractions(chatTaskCardRepository, chatMemberDirectoryPort);
    }
}
