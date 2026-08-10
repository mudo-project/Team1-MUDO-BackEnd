package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.file.application.usecase.GetFileDownloadUrlUseCase;
import com.academy.mudogroupware.messenger.application.command.SendMessageCommand;
import com.academy.mudogroupware.messenger.domain.event.ChatMessageSentEvent;
import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.model.MessageType;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

class SendMessageServiceTest {

    private static final LocalDateTime ROOM_CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 14, 30);

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final GetFileDownloadUrlUseCase getFileDownloadUrlUseCase = mock(GetFileDownloadUrlUseCase.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final SendMessageService service =
            new SendMessageService(chatRoomRepository, chatMessageRepository, getFileDownloadUrlUseCase,
                    eventPublisher, clock);

    @Test
    void sendsMessageWithClockBasedTimestampAndMarksSenderRead() {
        ChatRoom chatRoom = ChatRoom.restore(1L, 10L, null, ChatRoomType.DM, 1L,
                List.of(ChatRoomMember.restore(1L, null), ChatRoomMember.restore(2L, null)), ROOM_CREATED_AT);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            return ChatMessage.restore(5L, message.getChatRoomId(), message.getSenderUserId(),
                    message.getMessageType(), message.getContent(), message.getFileId(), message.getFileName(),
                    message.getCreatedAt(), message.getEditedAt(), message.getDeletedAt());
        });

        service.sendMessage(new SendMessageCommand(1L, 1L, MessageType.TEXT, "hello", null, null));

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getCreatedAt()).isEqualTo(NOW);
        verify(chatRoomRepository).markRead(1L, 1L, NOW);
        ArgumentCaptor<ChatMessageSentEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().chatRoomId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().messageId()).isEqualTo(5L);
        assertThat(eventCaptor.getValue().unreadCount()).isEqualTo(1L);
    }

    @Test
    void sendsImageMessageAndResolvesDownloadUrlForRealtimeEvent() {
        ChatRoom chatRoom = ChatRoom.restore(1L, 10L, null, ChatRoomType.DM, 1L,
                List.of(ChatRoomMember.restore(1L, null), ChatRoomMember.restore(2L, null)), ROOM_CREATED_AT);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            return ChatMessage.restore(5L, message.getChatRoomId(), message.getSenderUserId(),
                    message.getMessageType(), message.getContent(), message.getFileId(), message.getFileName(),
                    message.getCreatedAt(), message.getEditedAt(), message.getDeletedAt());
        });
        when(getFileDownloadUrlUseCase.getDownloadUrl(99L, 10L)).thenReturn("https://example.com/download/99");

        service.sendMessage(new SendMessageCommand(1L, 1L, MessageType.IMAGE, null, 99L, "photo.png"));

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFileId()).isEqualTo(99L);
        verify(getFileDownloadUrlUseCase).getDownloadUrl(99L, 10L);
        ArgumentCaptor<ChatMessageSentEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageSentEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().fileId()).isEqualTo(99L);
        assertThat(eventCaptor.getValue().fileDownloadUrl()).isEqualTo("https://example.com/download/99");
    }

    @Test
    void doesNotPublishEventWhenDownloadUrlLookupFails() {
        ChatRoom chatRoom = ChatRoom.restore(1L, 10L, null, ChatRoomType.DM, 1L,
                List.of(ChatRoomMember.restore(1L, null), ChatRoomMember.restore(2L, null)), ROOM_CREATED_AT);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            return ChatMessage.restore(5L, message.getChatRoomId(), message.getSenderUserId(),
                    message.getMessageType(), message.getContent(), message.getFileId(), message.getFileName(),
                    message.getCreatedAt(), message.getEditedAt(), message.getDeletedAt());
        });
        when(getFileDownloadUrlUseCase.getDownloadUrl(99L, 10L))
                .thenThrow(new RuntimeException("file not found"));

        assertThatThrownBy(() -> service.sendMessage(
                new SendMessageCommand(1L, 1L, MessageType.IMAGE, null, 99L, "photo.png")))
                .isInstanceOf(RuntimeException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
