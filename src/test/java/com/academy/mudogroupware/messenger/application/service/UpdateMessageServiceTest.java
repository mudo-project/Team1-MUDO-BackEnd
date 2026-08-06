package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.academy.mudogroupware.messenger.application.command.UpdateMessageCommand;
import com.academy.mudogroupware.messenger.domain.event.MessageEditedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.model.MessageType;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;

class UpdateMessageServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 14, 30);

    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final UpdateMessageService service =
            new UpdateMessageService(chatMessageRepository, eventPublisher, clock);

    @Test
    void senderCanEditOwnTextMessageAndEditedAtIsRecorded() {
        ChatMessage message = ChatMessage.restore(10L, 1L, 2L, MessageType.TEXT,
                "before", null, null, CREATED_AT, null, null);
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(new UpdateMessageCommand(1L, 10L, 2L, "after"));

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("after");
        assertThat(captor.getValue().getEditedAt()).isEqualTo(NOW);

        ArgumentCaptor<MessageEditedEvent> eventCaptor = ArgumentCaptor.forClass(MessageEditedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        MessageEditedEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(1L);
        assertThat(event.messageId()).isEqualTo(10L);
        assertThat(event.senderUserId()).isEqualTo(2L);
        assertThat(event.content()).isEqualTo("after");
        assertThat(event.editedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsEditByNonSender() {
        ChatMessage message = ChatMessage.restore(10L, 1L, 2L, MessageType.TEXT,
                "before", null, null, CREATED_AT, null, null);
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> service.update(new UpdateMessageCommand(1L, 10L, 3L, "after")))
                .isInstanceOf(MessengerException.class);
    }

    @Test
    void rejectsEditOfAlreadyDeletedMessage() {
        ChatMessage message = ChatMessage.restore(10L, 1L, 2L, MessageType.TEXT,
                "before", null, null, CREATED_AT, null, NOW);
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> service.update(new UpdateMessageCommand(1L, 10L, 2L, "after")))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.MESSAGE_ALREADY_DELETED);
    }

    @Test
    void rejectsEditOfNonTextMessage() {
        ChatMessage message = ChatMessage.restore(10L, 1L, 2L, MessageType.IMAGE,
                null, "https://example.com/a.png", "a.png", CREATED_AT, null, null);
        when(chatMessageRepository.findById(10L)).thenReturn(Optional.of(message));

        assertThatThrownBy(() -> service.update(new UpdateMessageCommand(1L, 10L, 2L, "after")))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.NOT_TEXT_MESSAGE);
    }
}
