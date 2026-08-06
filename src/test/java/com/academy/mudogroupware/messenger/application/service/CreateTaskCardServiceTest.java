package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.messenger.application.command.CreateTaskCardCommand;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCreatedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

class CreateTaskCardServiceTest {

    private static final LocalDateTime ROOM_CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 9, 0);

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatTaskCardRepository chatTaskCardRepository = mock(ChatTaskCardRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final CreateTaskCardService service =
            new CreateTaskCardService(chatRoomRepository, chatTaskCardRepository, eventPublisher, clock);

    @Test
    void createsTaskCardAndPublishesCreatedEvent() {
        ChatRoom chatRoom = ChatRoom.restore(1L, 10L, null, ChatRoomType.GROUP, 2L,
                List.of(ChatRoomMember.restore(2L, null), ChatRoomMember.restore(3L, null),
                        ChatRoomMember.restore(4L, null)), ROOM_CREATED_AT);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatTaskCardRepository.save(any(ChatTaskCard.class))).thenAnswer(invocation -> {
            ChatTaskCard card = invocation.getArgument(0);
            return ChatTaskCard.restore(7L, card.getChatRoomId(), card.getAssignerUserId(), card.getContent(),
                    card.getDueDate(), card.getAssignees(), card.getCreatedAt());
        });

        Long cardId = service.createTaskCard(new CreateTaskCardCommand(
                1L, 2L, "과제 제출", LocalDate.of(2026, 8, 10), List.of(3L, 4L)));

        assertThat(cardId).isEqualTo(7L);
        ArgumentCaptor<TaskCardCreatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCardCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().chatRoomId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().cardId()).isEqualTo(7L);
        assertThat(eventCaptor.getValue().assigneeIds()).containsExactly(3L, 4L);
        assertThat(eventCaptor.getValue().createdAt()).isEqualTo(NOW);
    }

    @Test
    void throwsAndPublishesNothingWhenAssigneeIsNotRoomMember() {
        ChatRoom chatRoom = ChatRoom.restore(1L, 10L, null, ChatRoomType.GROUP, 2L,
                List.of(ChatRoomMember.restore(2L, null), ChatRoomMember.restore(3L, null)), ROOM_CREATED_AT);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));

        assertThatThrownBy(() -> service.createTaskCard(new CreateTaskCardCommand(
                1L, 2L, "과제 제출", LocalDate.of(2026, 8, 10), List.of(3L, 4L))))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.NOT_ROOM_MEMBER);

        verify(chatTaskCardRepository, never()).save(any(ChatTaskCard.class));
        verify(eventPublisher, never()).publishEvent(any());
    }
}
