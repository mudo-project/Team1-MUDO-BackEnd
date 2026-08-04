package com.academy.mudogroupware.messenger.application.service;

import java.time.Clock;
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
    private final Clock clock;

    @Override
    public ChatMessagePageView getMessages(Long chatRoomId, Long requesterId, LocalDateTime cursorCreatedAt,
                                            Long cursorMessageId, int size) {
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

        boolean isFirstPage = cursorCreatedAt == null && cursorMessageId == null;
        if (isFirstPage) {
            chatRoom.markRead(requesterId, LocalDateTime.now(clock));
            chatRoomRepository.save(chatRoom);
        }

        return new ChatMessagePageView(messageViews, hasNext);
    }

    private ChatMessageView toMessageView(ChatMessage message, Map<Long, ChatMemberInfo> senders) {
        ChatMemberInfo sender = senders.get(message.getSenderUserId());
        return new ChatMessageView(message.getId(), message.getSenderUserId(),
                sender != null ? sender.name() : null, message.getMessageType(), message.getContent(),
                message.getFileUrl(), message.getFileName(), message.getCreatedAt());
    }
}
