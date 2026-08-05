package com.academy.mudogroupware.messenger.domain.event;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.model.MessageType;

public record ChatMessageSentEvent(
        Long chatRoomId,
        Long messageId,
        Long senderUserId,
        MessageType messageType,
        String content,
        String fileUrl,
        String fileName,
        LocalDateTime createdAt,
        long unreadCount
) {
}
