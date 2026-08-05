package com.academy.mudogroupware.messenger.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.query.ChatMessagePageView;
import com.academy.mudogroupware.messenger.application.query.ChatMessageView;
import com.academy.mudogroupware.messenger.application.usecase.ChatMessageQueryUseCase;
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
public class ChatMessageQueryService implements ChatMessageQueryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberDirectoryPort chatMemberDirectoryPort;

    @Override
    public ChatMessagePageView getMessages(Long chatRoomId, Long requesterId, LocalDateTime cursorCreatedAt,
                                            Long cursorMessageId, int size) {
        if (size < 1 || size > 100) {
            throw new MessengerException(MessengerErrorCode.INVALID_PAGE_SIZE);
        }

        boolean cursorProvided = cursorCreatedAt != null || cursorMessageId != null;
        boolean cursorComplete = cursorCreatedAt != null && cursorMessageId != null;
        if (cursorProvided && !cursorComplete) {
            throw new MessengerException(MessengerErrorCode.INVALID_CURSOR);
        }

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.CHAT_ROOM_NOT_FOUND));
        if (!chatRoom.isMember(requesterId)) {
            throw new MessengerException(MessengerErrorCode.NOT_ROOM_MEMBER);
        }

        List<ChatMessage> fetched = chatMessageRepository.findByChatRoomId(chatRoomId, cursorCreatedAt,
                cursorMessageId, size);
        boolean hasNext = fetched.size() > size;
        List<ChatMessage> pageMessages = hasNext ? fetched.subList(0, size) : fetched;

        List<Long> senderIds = pageMessages.stream().map(ChatMessage::getSenderUserId).distinct().toList();
        Map<Long, ChatMemberInfo> senders = chatMemberDirectoryPort.getMembers(senderIds);
        List<ChatMessageView> messageViews = pageMessages.stream()
                .map(message -> toMessageView(message, senders))
                .toList();

        boolean isFirstPage = !cursorProvided;
        if (isFirstPage && !pageMessages.isEmpty()) {
            LocalDateTime readAt = pageMessages.get(0).getCreatedAt();
            chatRoom.markRead(requesterId, readAt);
            chatRoomRepository.markRead(chatRoomId, requesterId, readAt);
        }

        ChatMessage lastInPage = pageMessages.isEmpty() ? null : pageMessages.get(pageMessages.size() - 1);
        LocalDateTime nextCursorCreatedAt = hasNext ? lastInPage.getCreatedAt() : null;
        Long nextCursorMessageId = hasNext ? lastInPage.getId() : null;

        return new ChatMessagePageView(messageViews, hasNext, nextCursorCreatedAt, nextCursorMessageId);
    }

    private ChatMessageView toMessageView(ChatMessage message, Map<Long, ChatMemberInfo> senders) {
        ChatMemberInfo sender = senders.get(message.getSenderUserId());
        return new ChatMessageView(message.getId(), message.getSenderUserId(),
                sender != null ? sender.name() : null, message.getMessageType(), message.getContent(),
                message.getFileUrl(), message.getFileName(), message.getCreatedAt());
    }
}
