package com.academy.mudogroupware.messenger.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.messenger.application.port.ChatMemberDirectoryPort;
import com.academy.mudogroupware.messenger.application.port.ChatMemberInfo;
import com.academy.mudogroupware.messenger.application.query.ChatMessagePageView;
import com.academy.mudogroupware.messenger.application.query.ChatMessageView;
import com.academy.mudogroupware.messenger.application.usecase.ChatMessageQueryUseCase;
import com.academy.mudogroupware.messenger.domain.event.ChatRoomReadEvent;
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
public class ChatMessageQueryService implements ChatMessageQueryUseCase {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMemberDirectoryPort chatMemberDirectoryPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public ChatMessagePageView getMessages(Long chatRoomId, Long requesterId, LocalDateTime cursorCreatedAt,
                                            Long cursorMessageId, int size) {
        log.info("event=chat_message_list_시작 chatRoomId={}, requesterId={}", chatRoomId, requesterId);
        try {
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

            boolean isFirstPage = !cursorProvided;
            if (isFirstPage && !pageMessages.isEmpty()) {
                LocalDateTime readAt = pageMessages.get(0).getCreatedAt();
                chatRoom.markRead(requesterId, readAt);
                chatRoomRepository.markRead(chatRoomId, requesterId, readAt);
                eventPublisher.publishEvent(new ChatRoomReadEvent(chatRoomId, requesterId, readAt));
            }

            List<Long> senderIds = pageMessages.stream().map(ChatMessage::getSenderUserId).distinct().toList();
            Map<Long, ChatMemberInfo> senders = chatMemberDirectoryPort.getMembers(senderIds);
            List<Long> messageIds = pageMessages.stream().map(ChatMessage::getId).toList();
            Map<Long, Long> unreadCounts = chatMessageRepository.countUnreadByMessageIds(chatRoomId, messageIds);
            List<ChatMessageView> messageViews = pageMessages.stream()
                    .map(message -> toMessageView(message, senders, unreadCounts))
                    .toList();

            ChatMessage lastInPage = pageMessages.isEmpty() ? null : pageMessages.get(pageMessages.size() - 1);
            LocalDateTime nextCursorCreatedAt = hasNext ? lastInPage.getCreatedAt() : null;
            Long nextCursorMessageId = hasNext ? lastInPage.getId() : null;

            log.info("event=chat_message_list_완료 chatRoomId={}, requesterId={}, count={}, hasNext={}", chatRoomId,
                    requesterId, messageViews.size(), hasNext);
            return new ChatMessagePageView(messageViews, hasNext, nextCursorCreatedAt, nextCursorMessageId);
        } catch (RuntimeException e) {
            log.warn("event=chat_message_list_실패 chatRoomId={}, requesterId={}, reason={}", chatRoomId,
                    requesterId, e.getMessage());
            throw e;
        }
    }

    private ChatMessageView toMessageView(ChatMessage message, Map<Long, ChatMemberInfo> senders,
                                          Map<Long, Long> unreadCounts) {
        ChatMemberInfo sender = senders.get(message.getSenderUserId());
        return new ChatMessageView(message.getId(), message.getSenderUserId(),
                sender != null ? sender.name() : null, message.getMessageType(), message.getContent(),
                message.getFileUrl(), message.getFileName(), message.getCreatedAt(), message.getEditedAt(),
                message.getDeletedAt(), unreadCounts.getOrDefault(message.getId(), 0L));
    }
}
