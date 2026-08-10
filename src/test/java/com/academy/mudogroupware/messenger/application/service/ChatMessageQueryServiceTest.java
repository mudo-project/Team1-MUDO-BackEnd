package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.file.application.usecase.GetFileDownloadUrlUseCase;
import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.query.ChatMessagePageView;
import com.academy.mudogroupware.messenger.domain.event.ChatRoomReadEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.model.MessageType;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

class ChatMessageQueryServiceTest {

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatMemberDirectoryPort chatMemberDirectoryPort = mock(ChatMemberDirectoryPort.class);
    private final GetFileDownloadUrlUseCase getFileDownloadUrlUseCase = mock(GetFileDownloadUrlUseCase.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ChatMessageQueryService service =
            new ChatMessageQueryService(chatRoomRepository, chatMessageRepository, chatMemberDirectoryPort,
                    getFileDownloadUrlUseCase, eventPublisher);

    @Test
    void rejectsOversizedMessagePageSizeBeforeQuerying() {
        assertThatThrownBy(() -> service.getMessages(1L, 1L, null, null, 101))
                .isInstanceOf(MessengerException.class);

        verifyNoInteractions(chatRoomRepository, chatMessageRepository, chatMemberDirectoryPort,
                getFileDownloadUrlUseCase, eventPublisher);
    }

    @Test
    void includesUnreadCountForEachMessage() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        ChatRoom chatRoom = ChatRoom.restore(1L, "group", ChatRoomType.GROUP, 1L,
                List.of(ChatRoomMember.restore(1L, createdAt), ChatRoomMember.restore(2L, null)),
                createdAt.minusHours(1));
        ChatMessage first = ChatMessage.restore(10L, 1L, 1L, MessageType.TEXT,
                "first", null, null, createdAt, null, null);
        ChatMessage second = ChatMessage.restore(11L, 1L, 1L, MessageType.TEXT,
                "second", null, null, createdAt.plusMinutes(1), null, null);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByChatRoomId(1L, null, null, 20)).thenReturn(List.of(first, second));
        when(chatMemberDirectoryPort.getMembers(List.of(1L))).thenReturn(
                Map.of(1L, new ChatMemberInfo(1L, "sender", 10L)));
        when(chatMessageRepository.countUnreadByMessageIds(1L, List.of(10L, 11L)))
                .thenReturn(Map.of(10L, 1L, 11L, 2L));

        ChatMessagePageView view = service.getMessages(1L, 1L, null, null, 20);

        org.assertj.core.api.Assertions.assertThat(view.messages().get(0).unreadCount()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(view.messages().get(1).unreadCount()).isEqualTo(2L);
    }

    @Test
    void resolvesDownloadUrlsForImageAndFileMessagesInBatch() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        ChatRoom chatRoom = ChatRoom.restore(1L, "group", ChatRoomType.GROUP, 1L,
                List.of(ChatRoomMember.restore(1L, createdAt), ChatRoomMember.restore(2L, null)),
                createdAt.minusHours(1));
        ChatMessage textMessage = ChatMessage.restore(10L, 1L, 1L, MessageType.TEXT,
                "first", null, null, createdAt, null, null);
        ChatMessage imageMessage = ChatMessage.restore(11L, 1L, 1L, MessageType.IMAGE,
                null, 99L, "photo.png", createdAt.plusMinutes(1), null, null);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByChatRoomId(1L, null, null, 20))
                .thenReturn(List.of(textMessage, imageMessage));
        when(chatMemberDirectoryPort.getMembers(List.of(1L))).thenReturn(
                Map.of(1L, new ChatMemberInfo(1L, "sender", 10L)));
        when(chatMemberDirectoryPort.getMember(1L)).thenReturn(new ChatMemberInfo(1L, "sender", 10L));
        when(chatMessageRepository.countUnreadByMessageIds(1L, List.of(10L, 11L)))
                .thenReturn(Map.of(10L, 1L, 11L, 1L));
        when(getFileDownloadUrlUseCase.getDownloadUrls(List.of(99L), 10L))
                .thenReturn(Map.of(99L, "https://example.com/download/99"));

        ChatMessagePageView view = service.getMessages(1L, 1L, null, null, 20);

        verify(getFileDownloadUrlUseCase).getDownloadUrls(List.of(99L), 10L);
        org.assertj.core.api.Assertions.assertThat(view.messages().get(0).fileDownloadUrl()).isNull();
        org.assertj.core.api.Assertions.assertThat(view.messages().get(1).fileDownloadUrl())
                .isEqualTo("https://example.com/download/99");
    }

    @Test
    void excludesDeletedMessagesFromDownloadUrlBatchLookup() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        ChatRoom chatRoom = ChatRoom.restore(1L, "group", ChatRoomType.GROUP, 1L,
                List.of(ChatRoomMember.restore(1L, createdAt), ChatRoomMember.restore(2L, null)),
                createdAt.minusHours(1));
        ChatMessage deletedImageMessage = ChatMessage.restore(10L, 1L, 1L, MessageType.IMAGE,
                null, 77L, "old.png", createdAt, null, createdAt.plusMinutes(5));
        ChatMessage activeImageMessage = ChatMessage.restore(11L, 1L, 1L, MessageType.IMAGE,
                null, 99L, "photo.png", createdAt.plusMinutes(1), null, null);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByChatRoomId(1L, null, null, 20))
                .thenReturn(List.of(deletedImageMessage, activeImageMessage));
        when(chatMemberDirectoryPort.getMembers(List.of(1L))).thenReturn(
                Map.of(1L, new ChatMemberInfo(1L, "sender", 10L)));
        when(chatMemberDirectoryPort.getMember(1L)).thenReturn(new ChatMemberInfo(1L, "sender", 10L));
        when(chatMessageRepository.countUnreadByMessageIds(1L, List.of(10L, 11L)))
                .thenReturn(Map.of(10L, 1L, 11L, 1L));
        when(getFileDownloadUrlUseCase.getDownloadUrls(List.of(99L), 10L))
                .thenReturn(Map.of(99L, "https://example.com/download/99"));

        service.getMessages(1L, 1L, null, null, 20);

        verify(getFileDownloadUrlUseCase).getDownloadUrls(List.of(99L), 10L);
    }

    @Test
    void publishesReadEventWhenFirstPageMarksRoomRead() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 0);
        ChatRoom chatRoom = ChatRoom.restore(1L, "group", ChatRoomType.GROUP, 1L,
                List.of(ChatRoomMember.restore(1L, null), ChatRoomMember.restore(2L, null)),
                createdAt.minusHours(1));
        ChatMessage message = ChatMessage.restore(10L, 1L, 1L, MessageType.TEXT,
                "first", null, null, createdAt, null, null);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatMessageRepository.findByChatRoomId(1L, null, null, 20)).thenReturn(List.of(message));
        when(chatMemberDirectoryPort.getMembers(List.of(1L))).thenReturn(
                Map.of(1L, new ChatMemberInfo(1L, "sender", 10L)));
        when(chatMessageRepository.countUnreadByMessageIds(1L, List.of(10L))).thenReturn(Map.of(10L, 1L));

        service.getMessages(1L, 2L, null, null, 20);

        ArgumentCaptor<ChatRoomReadEvent> captor = ArgumentCaptor.forClass(ChatRoomReadEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().chatRoomId()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().readerUserId()).isEqualTo(2L);
        org.assertj.core.api.Assertions.assertThat(captor.getValue().readAt()).isEqualTo(createdAt);
    }
}
