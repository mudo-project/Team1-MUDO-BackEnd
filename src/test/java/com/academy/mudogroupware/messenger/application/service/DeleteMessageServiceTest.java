package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.messenger.domain.event.MessageDeletedEvent;
import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.model.MessageType;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;

class DeleteMessageServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 14, 30);

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final DeleteMessageService service =
            new DeleteMessageService(chatMessageRepository, eventPublisher, clock);

    @Test
    void senderCanSoftDeleteOwnMessageAndDeletedAtIsRecorded() {
        ChatMessage message = ChatMessage.restore(10L, 1L, 2L, MessageType.TEXT,
                "hello", null, null, CREATED_AT, null, null);
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(1L, 10L, 2L);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isEqualTo(NOW);
        assertThat(captor.getValue().isDeleted()).isTrue();

        ArgumentCaptor<MessageDeletedEvent> eventCaptor = ArgumentCaptor.forClass(MessageDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        MessageDeletedEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(1L);
        assertThat(event.messageId()).isEqualTo(10L);
        assertThat(event.deleterUserId()).isEqualTo(2L);
        assertThat(event.deletedAt()).isEqualTo(NOW);
    }
}
