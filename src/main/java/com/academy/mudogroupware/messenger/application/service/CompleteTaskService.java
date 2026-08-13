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

        LocalDateTime completedAt = LocalDateTime.now(clock);
        chatTaskCard.complete(command.userId(), completedAt);

        // markAssigneeCompleted가 0건이면 이유가 두 가지다: (1) 담당자가 이미 완료된 상태였음(정상,
        // idempotent) (2) 이 트랜잭션이 카드를 읽은 뒤 다른 트랜잭션이 먼저 삭제를 커밋함. 조회 시점의
        // 완료 여부로는 이 둘을 구분할 수 없다(동시에 같은 완료 요청이 두 번 오면 둘 다 조회 시점엔
        // 미완료였다가 하나만 반영되므로) — 0건일 때만 락을 걸어 카드의 현재 삭제 여부를 다시 확인해서
        // 진짜 이유를 가린다.
        boolean updated = chatTaskCardRepository.markAssigneeCompleted(command.cardId(), command.userId(),
                completedAt);
        if (!updated && chatTaskCardRepository.isDeleted(command.cardId())) {
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
