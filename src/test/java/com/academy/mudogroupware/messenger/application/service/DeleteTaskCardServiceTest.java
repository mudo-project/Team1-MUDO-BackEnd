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

import com.academy.mudogroupware.messenger.application.command.DeleteTaskCardCommand;
import com.academy.mudogroupware.messenger.domain.event.TaskCardDeletedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskAssignee;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

class DeleteTaskCardServiceTest {

    private static final LocalDateTime CARD_CREATED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 9, 0);

    private final ChatTaskCardRepository chatTaskCardRepository = mock(ChatTaskCardRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final DeleteTaskCardService service =
            new DeleteTaskCardService(chatTaskCardRepository, eventPublisher, clock);

    @Test
    void ownerCanSoftDeleteAndPublishesDeletedEvent() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", null,
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        when(chatTaskCardRepository.markDeleted(7L, NOW)).thenReturn(true);

        service.delete(new DeleteTaskCardCommand(1L, 7L, 2L));

        verify(chatTaskCardRepository).markDeleted(7L, NOW);
        ArgumentCaptor<TaskCardDeletedEvent> eventCaptor = ArgumentCaptor.forClass(TaskCardDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().chatRoomId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().cardId()).isEqualTo(7L);
        assertThat(eventCaptor.getValue().deletedAt()).isEqualTo(NOW);
    }

    @Test
    void throwsAndWritesNothingWhenRequesterIsNotOwner() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", null,
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));

        assertThatThrownBy(() -> service.delete(new DeleteTaskCardCommand(1L, 7L, 3L)))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.NOT_TASK_CARD_OWNER);

        verify(chatTaskCardRepository, never()).markDeleted(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void throwsAndWritesNothingWhenAnyAssigneeAlreadyCompleted() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", null,
                List.of(ChatTaskAssignee.restore(3L, CARD_CREATED_AT)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));

        assertThatThrownBy(() -> service.delete(new DeleteTaskCardCommand(1L, 7L, 2L)))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.TASK_CARD_HAS_COMPLETION);

        verify(chatTaskCardRepository, never()).markDeleted(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void reDeleteByOwnerSucceedsSilentlyWithoutDuplicateWriteOrEvent() {
        LocalDateTime firstDeletedAt = LocalDateTime.of(2026, 8, 5, 12, 0);
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", null,
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT, firstDeletedAt);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        when(chatTaskCardRepository.markDeleted(7L, firstDeletedAt)).thenReturn(false);
        when(chatTaskCardRepository.isDeleted(7L)).thenReturn(true);

        service.delete(new DeleteTaskCardCommand(1L, 7L, 2L));

        verify(chatTaskCardRepository).markDeleted(7L, firstDeletedAt);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void reDeleteByOwnerSucceedsSilentlyEvenWhenAlreadyDeletedCardHasCompletedAssignee() {
        LocalDateTime firstDeletedAt = LocalDateTime.of(2026, 8, 5, 12, 0);
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", null,
                List.of(ChatTaskAssignee.restore(3L, CARD_CREATED_AT)), CARD_CREATED_AT, firstDeletedAt);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        when(chatTaskCardRepository.markDeleted(7L, firstDeletedAt)).thenReturn(false);
        when(chatTaskCardRepository.isDeleted(7L)).thenReturn(true);

        service.delete(new DeleteTaskCardCommand(1L, 7L, 2L));

        verify(chatTaskCardRepository).markDeleted(7L, firstDeletedAt);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void concurrentDeleteThatLosesTheRaceSkipsEvent() {
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", null,
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        // 다른 트랜잭션이 먼저 삭제를 커밋해서 이 요청의 UPDATE는 0건에 영향을 준 상황을 흉내낸다.
        when(chatTaskCardRepository.markDeleted(7L, NOW)).thenReturn(false);
        when(chatTaskCardRepository.isDeleted(7L)).thenReturn(true);

        service.delete(new DeleteTaskCardCommand(1L, 7L, 2L));

        verify(chatTaskCardRepository).markDeleted(7L, NOW);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void throwsTaskCardHasCompletionWhenConcurrentCompletionWinsTheRace() {
        // CodeRabbit 지적 사항(#457): 조회 시점엔 완료된 담당자가 없어 도메인 검증은 통과하지만,
        // 이 트랜잭션이 카드를 읽은 뒤 다른 트랜잭션이 먼저 완료를 커밋해서 markDeleted가 0건인 상황.
        // 카드가 실제로 삭제된 게 아니므로 조용히 넘어가지 말고 TASK_CARD_HAS_COMPLETION을 던져야 한다.
        ChatTaskCard chatTaskCard = ChatTaskCard.restore(7L, 1L, 2L, "과제 제출", null,
                List.of(ChatTaskAssignee.restore(3L, null)), CARD_CREATED_AT);
        when(chatTaskCardRepository.findById(7L)).thenReturn(Optional.of(chatTaskCard));
        when(chatTaskCardRepository.markDeleted(7L, NOW)).thenReturn(false);
        when(chatTaskCardRepository.isDeleted(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(new DeleteTaskCardCommand(1L, 7L, 2L)))
                .isInstanceOf(MessengerException.class)
                .extracting(exception -> ((MessengerException) exception).getErrorCode())
                .isEqualTo(MessengerErrorCode.TASK_CARD_HAS_COMPLETION);

        verify(chatTaskCardRepository).markDeleted(7L, NOW);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
