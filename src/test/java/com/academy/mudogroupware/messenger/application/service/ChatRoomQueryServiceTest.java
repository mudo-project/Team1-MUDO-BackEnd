package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.query.ChatRoomSummaryView;
import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.model.MessageType;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

class ChatRoomQueryServiceTest {

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatMemberDirectoryPort chatMemberDirectoryPort = mock(ChatMemberDirectoryPort.class);
    private final ChatRoomQueryService service =
            new ChatRoomQueryService(chatRoomRepository, chatMessageRepository, chatMemberDirectoryPort);

    @Test
    void masksDeletedLatestMessageInRoomPreview() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        ChatRoom chatRoom = ChatRoom.restore(1L, "group", ChatRoomType.GROUP, 1L,
                List.of(ChatRoomMember.restore(1L, null), ChatRoomMember.restore(2L, null)),
                createdAt.minusHours(1));
        ChatMessage deletedLatestMessage = ChatMessage.restore(10L, 1L, 2L, MessageType.TEXT,
                "민감한 내용", null, null, createdAt, null, createdAt.plusMinutes(1));
        when(chatRoomRepository.findAllByMember(1L)).thenReturn(List.of(chatRoom));
        when(chatMemberDirectoryPort.getMembers(List.of())).thenReturn(Map.of());
        when(chatMessageRepository.findLatestByChatRoomIds(List.of(1L)))
                .thenReturn(Map.of(1L, deletedLatestMessage));
        when(chatMessageRepository.countUnreadByRequester(1L, List.of(1L))).thenReturn(Map.of());

        List<ChatRoomSummaryView> views = service.getRooms(1L);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).lastMessagePreview()).isEqualTo("삭제된 메시지입니다.");
    }

    @Test
    void showsActualContentWhenLatestMessageIsNotDeleted() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        ChatRoom chatRoom = ChatRoom.restore(1L, "group", ChatRoomType.GROUP, 1L,
                List.of(ChatRoomMember.restore(1L, null), ChatRoomMember.restore(2L, null)),
                createdAt.minusHours(1));
        ChatMessage latestMessage = ChatMessage.restore(10L, 1L, 2L, MessageType.TEXT,
                "안녕하세요", null, null, createdAt, null, null);
        when(chatRoomRepository.findAllByMember(1L)).thenReturn(List.of(chatRoom));
        when(chatMemberDirectoryPort.getMembers(List.of())).thenReturn(Map.of());
        when(chatMessageRepository.findLatestByChatRoomIds(List.of(1L)))
                .thenReturn(Map.of(1L, latestMessage));
        when(chatMessageRepository.countUnreadByRequester(1L, List.of(1L))).thenReturn(Map.of());

        List<ChatRoomSummaryView> views = service.getRooms(1L);

        assertThat(views.get(0).lastMessagePreview()).isEqualTo("안녕하세요");
    }
}
