package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.UpdateMessageCommand;
import com.academy.mudogroupware.messenger.application.usecase.UpdateMessageUseCase;
import com.academy.mudogroupware.messenger.domain.event.MessageEditedEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMessageService implements UpdateMessageUseCase {

    private final ChatMessageRepository chatMessageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void update(UpdateMessageCommand command) {
        log.info("event=message_update_시작 chatRoomId={}, messageId={}, requesterId={}", command.chatRoomId(),
                command.messageId(), command.requesterId());
        try {
            ChatMessage message = chatMessageRepository.findById(command.messageId())
                    .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
            if (!message.getChatRoomId().equals(command.chatRoomId())) {
                throw new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND);
            }

            message.editText(command.requesterId(), command.content(), LocalDateTime.now(clock));
            chatMessageRepository.save(message);
            eventPublisher.publishEvent(new MessageEditedEvent(message.getChatRoomId(), message.getId(),
                    message.getSenderUserId(), message.getContent(), message.getEditedAt()));
            log.info("event=message_update_완료 chatRoomId={}, messageId={}, requesterId={}", command.chatRoomId(),
                    command.messageId(), command.requesterId());
        } catch (RuntimeException e) {
            log.warn("event=message_update_실패 chatRoomId={}, messageId={}, requesterId={}, reason={}",
                    command.chatRoomId(), command.messageId(), command.requesterId(), e.getMessage());
            throw e;
        }
    }
}
