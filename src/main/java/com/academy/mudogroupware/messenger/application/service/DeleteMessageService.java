package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.usecase.DeleteMessageUseCase;
import com.academy.mudogroupware.messenger.domain.event.MessageDeletedEvent;
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
public class DeleteMessageService implements DeleteMessageUseCase {

    private final ChatMessageRepository chatMessageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void delete(Long chatRoomId, Long messageId, Long requesterId) {
        log.info("event=message_delete_시작 chatRoomId={}, messageId={}, requesterId={}", chatRoomId, messageId,
                requesterId);
        try {
            ChatMessage message = chatMessageRepository.findById(messageId)
                    .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
            if (!message.getChatRoomId().equals(chatRoomId)) {
                throw new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND);
            }

            message.delete(requesterId, LocalDateTime.now(clock));
            chatMessageRepository.save(message);
            eventPublisher.publishEvent(new MessageDeletedEvent(message.getChatRoomId(), message.getId(),
                    requesterId, message.getDeletedAt()));
            log.info("event=message_delete_완료 chatRoomId={}, messageId={}, requesterId={}", chatRoomId, messageId,
                    requesterId);
        } catch (RuntimeException e) {
            log.warn("event=message_delete_실패 chatRoomId={}, messageId={}, requesterId={}, reason={}", chatRoomId,
                    messageId, requesterId, e.getMessage());
            throw e;
        }
    }
}
