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

import com.academy.mudogroupware.messenger.application.command.CompleteTaskCommand;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCompletedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
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
        when(chatTaskCardRepository.markAssigneeCompleted(7L, 3L, NOW)).thenReturn(true);

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
        when(chatTaskCardRepository.markAssigneeCompleted(7L, 4L, NOW)).thenReturn(true);

        service.complete(new CompleteTaskCommand(1L, 7L, 4L));

        ArgumentCaptor<TaskCardCompletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCardCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TaskCardCompletedEvent event = eventCaptor.getValue();
        assertThat(event.completedCount()).isEqualTo(2L);
        assertThat(event.assigneeCount()).isEqualTo(2);
        assertThat(event.fullyCompleted()).isTrue();
    }

    @Test
    void throwsTaskCardAlreadyDeletedWhenCardAlreadyDeletedBeforeRequest() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", LocalDate.of(2026, 8, 10),
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT,
                LocalDateTime.of(2026, 8, 6, 9, 0));
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));

        assertThatThrownBy(() -> service.complete(new CompleteTaskCommand(1L, 7L, 3L)))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.TASK_CARD_ALREADY_DELETED);

        verify(chatTaskCardRepository, never()).markAssigneeCompleted(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void throwsTaskCardAlreadyDeletedWhenConcurrentDeleteWinsTheRace() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", LocalDate.of(2026, 8, 10),
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        // 다른 트랜잭션이 먼저 삭제를 커밋해서 이 요청의 UPDATE는 0건에 영향을 준 상황을 흉내낸다.
        when(chatTaskCardRepository.markAssigneeCompleted(7L, 3L, NOW)).thenReturn(false);
        when(chatTaskCardRepository.isDeleted(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.complete(new CompleteTaskCommand(1L, 7L, 3L)))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.TASK_CARD_ALREADY_DELETED);

        verify(chatTaskCardRepository).markAssigneeCompleted(7L, 3L, NOW);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void idempotentReCompleteByAlreadyCompletedAssigneeDoesNotThrowEvenWhenUpdateAffectsNoRows() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", LocalDate.of(2026, 8, 10),
                List.of(ChatTaskAssignee.restore(3L, CARD_CREATED_AT)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        // completed_at is null 조건에 걸려 이미 완료된 담당자의 재요청은 원래도 0건 갱신이다(정상 흐름).
        when(chatTaskCardRepository.markAssigneeCompleted(7L, 3L, NOW)).thenReturn(false);
        when(chatTaskCardRepository.isDeleted(7L)).thenReturn(false);

        service.complete(new CompleteTaskCommand(1L, 7L, 3L));

        verify(chatTaskCardRepository).markAssigneeCompleted(7L, 3L, NOW);
    }

    @Test
    void concurrentFirstCompletionRequestThatLosesTheRaceDoesNotThrowWhenCardIsNotDeleted() {
        // CodeRabbit 지적 사항: 같은 담당자가 완료 요청을 동시에 두 번 보내면 둘 다 조회 시점엔
        // 미완료 상태(wasAlreadyCompleted=false)라서, 하나만 반영되고 나머지 하나는 0건 갱신이다.
        // 이 경우 카드가 삭제된 게 아니므로 예외를 던지면 안 된다.
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", LocalDate.of(2026, 8, 10),
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        when(chatTaskCardRepository.markAssigneeCompleted(7L, 3L, NOW)).thenReturn(false);
        when(chatTaskCardRepository.isDeleted(7L)).thenReturn(false);

        service.complete(new CompleteTaskCommand(1L, 7L, 3L));

        verify(chatTaskCardRepository).markAssigneeCompleted(7L, 3L, NOW);
        verify(chatTaskCardRepository).isDeleted(7L);
    }
}
