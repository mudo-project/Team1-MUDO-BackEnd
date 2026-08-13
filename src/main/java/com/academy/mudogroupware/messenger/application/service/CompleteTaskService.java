package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.CompleteTaskCommand;
import com.academy.mudogroupware.messenger.application.usecase.CompleteTaskUseCase;
import com.academy.mudogroupware.messenger.domain.event.TaskCardCompletedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskAssignee;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompleteTaskService implements CompleteTaskUseCase {

    private final ChatTaskCardRepository chatTaskCardRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void complete(CompleteTaskCommand command) {
        log.info("event=task_card_complete_시작 chatRoomId={}, cardId={}, userId={}", command.chatRoomId(),
                command.cardId(), command.userId());
        ChatTaskCard chatTaskCard = chatTaskCardRepository.findById(command.cardId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND));
        if (!chatTaskCard.getChatRoomId().equals(command.chatRoomId())) {
            throw new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND);
        }

        boolean wasAlreadyCompleted = chatTaskCard.getAssignees().stream()
                .filter(assignee -> assignee.getUserId().equals(command.userId()))
                .findFirst()
                .map(ChatTaskAssignee::isCompleted)
                .orElse(false);

        LocalDateTime completedAt = LocalDateTime.now(clock);
        chatTaskCard.complete(command.userId(), completedAt);

        // markAssigneeCompleted는 deleted_at is null 조건의 UPDATE라 행을 잠근다. 처음 완료하는
        // 요청인데도 0건이면, 이 트랜잭션이 카드를 읽은 뒤 다른 트랜잭션이 먼저 삭제를 커밋했다는 뜻이므로
        // 이벤트 발행 없이 여기서 중단한다(이미 완료된 담당자의 재요청은 원래도 0건이라 이 경우와 구분한다).
        boolean updated = chatTaskCardRepository.markAssigneeCompleted(command.cardId(), command.userId(),
                completedAt);
        if (!wasAlreadyCompleted && !updated) {
            throw new MessengerException(MessengerErrorCode.TASK_CARD_ALREADY_DELETED);
        }

        eventPublisher.publishEvent(new TaskCardCompletedEvent(chatTaskCard.getChatRoomId(),
                chatTaskCard.getId(), command.userId(), completedAt, chatTaskCard.getCompletedCount(),
                chatTaskCard.getAssigneeCount(), chatTaskCard.isFullyCompleted()));
        log.info(
                "event=task_card_complete_완료 chatRoomId={}, cardId={}, userId={}, completedCount={}, "
                        + "fullyCompleted={}",
                command.chatRoomId(), command.cardId(), command.userId(), chatTaskCard.getCompletedCount(),
                chatTaskCard.isFullyCompleted());
    }
}
