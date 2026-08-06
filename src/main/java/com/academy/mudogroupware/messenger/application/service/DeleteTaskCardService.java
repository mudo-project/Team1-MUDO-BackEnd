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

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteTaskCardService implements DeleteTaskCardUseCase {

    private final ChatTaskCardRepository chatTaskCardRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void delete(DeleteTaskCardCommand command) {
        ChatTaskCard chatTaskCard = chatTaskCardRepository.findById(command.cardId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND));
        if (!chatTaskCard.getChatRoomId().equals(command.chatRoomId())) {
            throw new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND);
        }

        LocalDateTime deletedAt = LocalDateTime.now(clock);
        chatTaskCard.delete(command.requesterId(), deletedAt);
        chatTaskCardRepository.markDeleted(chatTaskCard.getId(), chatTaskCard.getDeletedAt());
        eventPublisher.publishEvent(new TaskCardDeletedEvent(chatTaskCard.getChatRoomId(), chatTaskCard.getId(),
                chatTaskCard.getDeletedAt()));
    }
}
