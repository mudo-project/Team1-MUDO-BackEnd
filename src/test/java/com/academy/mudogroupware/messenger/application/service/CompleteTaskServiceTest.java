package com.academy.mudogroupware.messenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

import com.academy.mudogroupware.messenger.application.command.CompleteTaskCommand;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCompletedEvent;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskAssignee;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

class CompleteTaskServiceTest {

    private static final LocalDateTime CARD_CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 9, 30);

    private final ChatTaskCardRepository chatTaskCardRepository = mock(ChatTaskCardRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final CompleteTaskService service =
            new CompleteTaskService(chatTaskCardRepository, eventPublisher, clock);

    @Test
    void completesAssigneeTaskAndPublishesCompletedEventWithProgress() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", LocalDate.of(2026, 8, 10),
                List.of(ChatTaskAssignee.restore(3L, null), ChatTaskAssignee.restore(4L, null)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));

        service.complete(new CompleteTaskCommand(1L, 7L, 3L));

        verify(chatTaskCardRepository).markAssigneeCompleted(7L, 3L, NOW);
        ArgumentCaptor<TaskCardCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCardCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TaskCardCompletedEvent event = eventCaptor.getValue();
        assertThat(event.chatRoomId()).isEqualTo(1L);
        assertThat(event.cardId()).isEqualTo(7L);
        assertThat(event.completedUserId()).isEqualTo(3L);
        assertThat(event.completedAt()).isEqualTo(NOW);
        assertThat(event.completedCount()).isEqualTo(1L);
        assertThat(event.assigneeCount()).isEqualTo(2);
        assertThat(event.fullyCompleted()).isFalse();
    }

    @Test
    void publishesFullyCompletedEventWhenLastAssigneeCompletes() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", LocalDate.of(2026, 8, 10),
                List.of(ChatTaskAssignee.restore(3L, CARD_CREATED_AT), ChatTaskAssignee.restore(4L, null)),
                CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));

        service.complete(new CompleteTaskCommand(1L, 7L, 4L));

        ArgumentCaptor<TaskCardCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCardCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TaskCardCompletedEvent event = eventCaptor.getValue();
        assertThat(event.completedCount()).isEqualTo(2L);
        assertThat(event.assigneeCount()).isEqualTo(2);
        assertThat(event.fullyCompleted()).isTrue();
    }
}
