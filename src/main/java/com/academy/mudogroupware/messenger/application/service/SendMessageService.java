package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.command.SendMessageCommand;
import com.academy.mudogroupware.messenger.application.usecase.SendMessageUseCase;
import com.academy.mudogroupware.messenger.domain.event.ChatMessageSentEvent;
import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;
import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.model.ChatRoom;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;
import com.academy.mudogroupware.messenger.domain.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SendMessageService implements SendMessageUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public Long sendMessage(SendMessageCommand command) {
        ChatRoom chatRoom = chatRoomRepository.findById(command.chatRoomId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!chatRoom.isMember(command.senderId())) {
            throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
        }

        LocalDateTime createdAt = LocalDateTime.now(clock);
        ChatMessage chatMessage = ChatMessage.create(command.chatRoomId(), command.senderId(),
                command.messageType(), command.content(), command.fileUrl(), command.fileName(), createdAt);
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        chatRoom.markRead(command.senderId(), saved.getCreatedAt());
        chatRoomRepository.markRead(command.chatRoomId(), command.senderId(), saved.getCreatedAt());
        eventPublisher.publishEvent(new ChatMessageSentEvent(saved.getChatRoomId(), saved.getId(),
                saved.getSenderUserId(), saved.getMessageType(), saved.getContent(), saved.getFileUrl(),
                saved.getFileName(), saved.getCreatedAt(), chatRoom.getMembers().size() - 1L));
        return saved.getId();
    }
}
