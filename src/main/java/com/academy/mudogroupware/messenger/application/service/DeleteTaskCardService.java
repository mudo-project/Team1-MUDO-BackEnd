package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.DeleteTaskCardCommand;
import com.academy.mudogroupware.messenger.application.usecase.DeleteTaskCardUseCase;
import com.academy.mudogroupware.messenger.domain.event.TaskCardDeletedEvent;
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
public class DeleteTaskCardService implements DeleteTaskCardUseCase {

    private final ChatTaskCardRepository chatTaskCardRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void delete(DeleteTaskCardCommand command) {
        log.info("event=task_card_delete_시작 chatRoomId={}, cardId={}, requesterId={}", command.chatRoomId(),
                command.cardId(), command.requesterId());
        try {
            ChatTaskCard chatTaskCard = chatTaskCardRepository.findById(command.cardId())
                    .orElseThrow(() -> new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND));
            if (!chatTaskCard.getChatRoomId().equals(command.chatRoomId())) {
                throw new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND);
            }

            // delete()는 이미 삭제된 카드엔 조용히 아무 것도 안 하는 idempotent 동작이다(권한 검증은 항상 수행).
            // 실제로 상태가 바뀐 경우에만 저장/이벤트 발행을 해서, 재요청 시 중복 브로드캐스트가 나가지 않게 한다.
            boolean alreadyDeleted = chatTaskCard.isDeleted();
            LocalDateTime deletedAt = LocalDateTime.now(clock);
            chatTaskCard.delete(command.requesterId(), deletedAt);
            if (alreadyDeleted) {
                log.info("event=task_card_delete_완료 chatRoomId={}, cardId={}, requesterId={}, alreadyDeleted=true",
                        command.chatRoomId(), command.cardId(), command.requesterId());
                return;
            }

            chatTaskCardRepository.markDeleted(chatTaskCard.getId(), chatTaskCard.getDeletedAt());
            eventPublisher.publishEvent(new TaskCardDeletedEvent(chatTaskCard.getChatRoomId(), chatTaskCard.getId(),
                    chatTaskCard.getDeletedAt()));
            log.info("event=task_card_delete_완료 chatRoomId={}, cardId={}, requesterId={}, alreadyDeleted=false",
                    command.chatRoomId(), command.cardId(), command.requesterId());
        } catch (RuntimeException e) {
            log.warn("event=task_card_delete_실패 chatRoomId={}, cardId={}, requesterId={}, reason={}",
                    command.chatRoomId(), command.cardId(), command.requesterId(), e.getMessage());
            throw e;
        }
    }
}
