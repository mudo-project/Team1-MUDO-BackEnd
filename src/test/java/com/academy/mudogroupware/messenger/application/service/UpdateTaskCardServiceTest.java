package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.messenger.application.command.UpdateTaskCardCommand;
import com.academy.mudogroupware.messenger.domain.event.TaskCardUpdatedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomMember;
import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskAssignee;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

class UpdateTaskCardServiceTest {

    private static final LocalDateTime ROOM_CREATED_AT = LocalDateTime.of(2026, 8, 1, 10, 0);
    private static final LocalDateTime CARD_CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);

    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatTaskCardRepository chatTaskCardRepository = mock(ChatTaskCardRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final UpdateTaskCardService service =
            new UpdateTaskCardService(chatRoomRepository, chatTaskCardRepository, eventPublisher);

    private ChatRoom roomWithMembers(Long... userIds) {
        List<ChatRoomMember> members = List.of(userIds).stream().map(id -> ChatRoomMember.restore(id, null))
                .toList();
        return ChatRoom.restore(1L, 10L, "그룹", ChatRoomType.GROUP, 2L, members, ROOM_CREATED_AT);
    }

    @Test
    void replacesContentAndDiffsAssigneesThenPublishesUpdatedEvent() {
        ChatRoom chatRoom = roomWithMembers(2L, 3L, 4L, 5L);
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "old", LocalDate.of(2026, 8, 10),
                List.of(ChatTaskAssignee.restore(3L, CARD_CREATED_AT), ChatTaskAssignee.restore(4L, null)),
                CARD_CREATED_AT);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        when(chatTaskCardRepository.updateContent(any(), any(), any())).thenReturn(true);

        service.update(new UpdateTaskCardCommand(1L, 7L, 2L, "new", LocalDate.of(2026, 8, 20), List.of(4L, 5L)));

        verify(chatTaskCardRepository).updateContent(7L, "new", LocalDate.of(2026, 8, 20));
        verify(chatTaskCardRepository).replaceAssignees(7L, List.of(5L), List.of(3L));

        ArgumentCaptor<TaskCardUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCardUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TaskCardUpdatedEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(1L);
        assertThat(event.cardId()).isEqualTo(7L);
        assertThat(event.content()).isEqualTo("new");
        assertThat(event.dueDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(event.assigneeIds()).containsExactly(4L, 5L);
    }

    @Test
    void throwsAndWritesNothingWhenRequesterIsNotOwner() {
        ChatRoom chatRoom = roomWithMembers(2L, 3L, 4L);
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "old", null,
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));

        assertThatThrownBy(() -> service.update(
                new UpdateTaskCardCommand(1L, 7L, 3L, "new", null, List.of(3L))))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.NOT_TASK_CARD_OWNER);

        verify(chatTaskCardRepository, never()).updateContent(any(), any(), any());
        verify(chatTaskCardRepository, never()).replaceAssignees(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void throwsAlreadyDeletedAndSkipsAssigneesAndEventWhenConcurrentlyDeleted() {
        ChatRoom chatRoom = roomWithMembers(2L, 3L, 4L);
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "old", null,
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT);
        when(chatRoomRepository.findById(1L)).thenReturn(Optional.of(chatRoom));
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        // deleted_at is null 조건의 UPDATE가 0건 반영됐다는 뜻 — 이 트랜잭션이 읽은 뒤 다른 트랜잭션이
        // 먼저 삭제를 커밋한 상황을 흉내낸다.
        when(chatTaskCardRepository.updateContent(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.update(
                new UpdateTaskCardCommand(1L, 7L, 2L, "new", null, List.of(3L, 4L))))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.TASK_CARD_ALREADY_DELETED);

        verify(chatTaskCardRepository, never()).replaceAssignees(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
