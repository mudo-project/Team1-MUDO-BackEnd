package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.messenger.domain.model.ChatMessage;
import com.academy.mudogroupware.messenger.domain.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final ChatMessageJpaRepository chatMessageJpaRepository;

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        ChatMessageEntity entity = toEntity(chatMessage);
        return toDomain(chatMessageJpaRepository.save(entity));
    }

    @Override
    public Optional<ChatMessage> findById(Long id) {
        return chatMessageJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<ChatMessage> findByChatRoomId(Long chatRoomId, LocalDateTime cursorCreatedAt, Long cursorMessageId,
                                               int size) {
        return chatMessageJpaRepository.findPage(chatRoomId, cursorCreatedAt, cursorMessageId,
                        PageRequest.of(0, size + 1))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<Long, Long> countUnreadByRequester(Long userId, List<Long> chatRoomIds) {
        if (chatRoomIds.isEmpty()) {
            return Map.of();
        }
        return chatMessageJpaRepository.countUnreadByRequester(userId, chatRoomIds).stream()
                .collect(Collectors.toMap(ChatRoomUnreadCountProjection::getChatRoomId,
                        ChatRoomUnreadCountProjection::getUnreadCount));
    }

    @Override
    public Map<Long, Long> countUnreadByMessageIds(Long chatRoomId, List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Map.of();
        }
        return chatMessageJpaRepository.countUnreadByMessageIds(chatRoomId, messageIds).stream()
                .collect(Collectors.toMap(MessageUnreadCountProjection::getMessageId,
                        MessageUnreadCountProjection::getUnreadCount));
    }

    @Override
    public Map<Long, ChatMessage> findLatestByChatRoomIds(List<Long> chatRoomIds) {
        return chatMessageJpaRepository.findLatestByChatRoomIds(chatRoomIds).stream()
                .map(this::toDomain)
                .collect(Collectors.toMap(ChatMessage::getChatRoomId, Function.identity(),
                        (a, b) -> a.getId() > b.getId() ? a : b));
    }

    private ChatMessageEntity toEntity(ChatMessage chatMessage) {
        return ChatMessageEntity.builder()
                .id(chatMessage.getId())
                .chatRoomId(chatMessage.getChatRoomId())
                .senderUserId(chatMessage.getSenderUserId())
                .messageType(chatMessage.getMessageType())
                .content(chatMessage.getContent())
                .fileUrl(chatMessage.getFileUrl())
                .fileName(chatMessage.getFileName())
                .createdAt(chatMessage.getCreatedAt())
                .editedAt(chatMessage.getEditedAt())
                .deletedAt(chatMessage.getDeletedAt())
                .build();
    }

    private ChatMessage toDomain(ChatMessageEntity entity) {
        return ChatMessage.restore(entity.getId(), entity.getChatRoomId(), entity.getSenderUserId(),
                entity.getMessageType(), entity.getContent(), entity.getFileUrl(), entity.getFileName(),
                entity.getCreatedAt(), entity.getEditedAt(), entity.getDeletedAt());
    }
}
