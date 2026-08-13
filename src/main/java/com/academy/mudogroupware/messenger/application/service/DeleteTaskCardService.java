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
        ChatTaskCard chatTaskCard = chatTaskCardRepository.findById(command.cardId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND));
        if (!chatTaskCard.getChatRoomId().equals(command.chatRoomId())) {
            throw new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND);
        }

        // delete()는 이미 삭제된 카드엔 조용히 아무 것도 안 하는 idempotent 동작이다(권한 검증은 항상 수행).
        // markDeleted는 deleted_at is null AND 완료된 담당자 없음 조건의 UPDATE라 행을 잠근다. 영향받은
        // 행이 없으면 이유가 두 가지다: (1) 이미 삭제됨(동시 삭제 요청 중 하나가 먼저 커밋) (2) 조회
        // 시점엔 완료된 담당자가 없었지만 이 트랜잭션이 카드를 읽은 뒤 다른 트랜잭션이 먼저 완료를
        // 커밋함. 조회 시점 상태만으로는 이 둘을 구분할 수 없으므로(메모리 상 상태로 판단하면 (2)를
        // 놓쳐 완료된 카드가 삭제될 수 있다), 0건일 때만 락을 걸어 카드의 현재 삭제 여부를 다시 확인한다.
        LocalDateTime deletedAt = LocalDateTime.now(clock);
        chatTaskCard.delete(command.requesterId(), deletedAt);
        boolean deleted = chatTaskCardRepository.markDeleted(chatTaskCard.getId(), chatTaskCard.getDeletedAt());
        if (!deleted) {
            if (!chatTaskCardRepository.isDeleted(chatTaskCard.getId())) {
                throw new MessengerException(MessengerErrorCode.TASK_CARD_HAS_COMPLETION);
            }
            log.info("event=task_card_delete_완료 chatRoomId={}, cardId={}, requesterId={}, alreadyDeleted=true",
                    command.chatRoomId(), command.cardId(), command.requesterId());
            return;
        }

        eventPublisher.publishEvent(new TaskCardDeletedEvent(chatTaskCard.getChatRoomId(), chatTaskCard.getId(),
                chatTaskCard.getDeletedAt()));
        log.info("event=task_card_delete_완료 chatRoomId={}, cardId={}, requesterId={}, alreadyDeleted=false",
                command.chatRoomId(), command.cardId(), command.requesterId());
    }
}
