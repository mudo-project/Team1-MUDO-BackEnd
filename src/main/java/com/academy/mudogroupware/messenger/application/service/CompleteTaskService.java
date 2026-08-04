package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.CompleteTaskCommand;
import com.academy.mudogroupware.messenger.application.usecase.CompleteTaskUseCase;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatTaskCard;
import com.academy.mudogroupware.messenger.domain.repository.ChatTaskCardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteTaskService implements CompleteTaskUseCase {

    private final ChatTaskCardRepository chatTaskCardRepository;
    private final Clock clock;

    @Override
    public void complete(CompleteTaskCommand command) {
        ChatTaskCard chatTaskCard = chatTaskCardRepository.findById(command.cardId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND));
        if (!chatTaskCard.getChatRoomId().equals(command.chatRoomId())) {
            throw new MessengerException(MessengerErrorCode.TASK_CARD_NOT_FOUND);
        }

        chatTaskCard.complete(command.userId(), LocalDateTime.now(clock));
        chatTaskCardRepository.save(chatTaskCard);
    }
}
