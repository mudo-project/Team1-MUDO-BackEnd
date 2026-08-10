package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.academy.mudogroupware.messenger.application.command.CreateChatRoomCommand;
import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

class CreateChatRoomServiceTest {

    private static final LocalDateTime ROOM_CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 14, 30);

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatMemberDirectoryPort chatMemberDirectoryPort = mock(ChatMemberDirectoryPort.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final CreateChatRoomService service =
            new CreateChatRoomService(chatRoomRepository, chatMemberDirectoryPort, clock);

    @Test
    void createsRoomWithClockBasedTimestamp() {
        when(chatMemberDirectoryPort.getMember(1L)).thenReturn(new ChatMemberInfo(1L, "requester", 10L));
        when(chatMemberDirectoryPort.getMembers(List.of(2L))).thenReturn(
                Map.of(2L, new ChatMemberInfo(2L, "participant", 10L)));
        when(chatRoomRepository.save(org.mockito.ArgumentMatchers.any(ChatRoom.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createRoom(new CreateChatRoomCommand(1L, List.of(2L), null));

        ArgumentCaptor<ChatRoom> captor = ArgumentCaptor.forClass(ChatRoom.class);
        org.mockito.Mockito.verify(chatRoomRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void reusesExistingDirectMessageRoomInsteadOfCreatingDuplicate() {
        ChatRoom existing = ChatRoom.restore(99L, null, ChatRoomType.DM, 1L,
                List.of(ChatRoomMember.restore(1L, null), ChatRoomMember.restore(2L, null)), ROOM_CREATED_AT);
        when(chatMemberDirectoryPort.getMember(1L)).thenReturn(new ChatMemberInfo(1L, "requester", 10L));
        when(chatMemberDirectoryPort.getMembers(List.of(2L))).thenReturn(
                Map.of(2L, new ChatMemberInfo(2L, "participant", 10L)));
        when(chatRoomRepository.findDirectMessage(1L, 2L)).thenReturn(Optional.of(existing));

        Long roomId = service.createRoom(new CreateChatRoomCommand(1L, List.of(2L), null));

        assertThat(roomId).isEqualTo(99L);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }
}
