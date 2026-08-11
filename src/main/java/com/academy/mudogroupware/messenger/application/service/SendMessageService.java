package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.file.application.usecase.GetFileDownloadUrlUseCase;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SendMessageService implements SendMessageUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GetFileDownloadUrlUseCase getFileDownloadUrlUseCase;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public Long sendMessage(SendMessageCommand command) {
        log.info("event=message_send_시작 chatRoomId={}, senderId={}", command.chatRoomId(), command.senderId());
        ChatRoom chatRoom = chatRoomRepository.findById(command.chatRoomId())
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!chatRoom.isMember(command.senderId())) {
            throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
        }

        LocalDateTime createdAt = LocalDateTime.now(clock);
        ChatMessage chatMessage = ChatMessage.create(command.chatRoomId(), command.senderId(),
                command.messageType(), command.content(), command.fileId(), command.fileName(), createdAt);
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        chatRoom.markRead(command.senderId(), saved.getCreatedAt());
        chatRoomRepository.markRead(command.chatRoomId(), command.senderId(), saved.getCreatedAt());
        String fileDownloadUrl = saved.getFileId() == null ? null
                : getFileDownloadUrlUseCase.getDownloadUrl(saved.getFileId());
        eventPublisher.publishEvent(new ChatMessageSentEvent(saved.getChatRoomId(), saved.getId(),
                saved.getSenderUserId(), saved.getMessageType(), saved.getContent(), saved.getFileId(),
                fileDownloadUrl, saved.getFileName(), saved.getCreatedAt(), chatRoom.getMembers().size() - 1L));
        log.info("event=message_send_완료 chatRoomId={}, senderId={}, messageId={}", command.chatRoomId(),
                command.senderId(), saved.getId());
        return saved.getId();
    }
}
