package com.academy.mudogroupware.messenger.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.model.MessageType;

public record ChatMessageView(
        Long id,
        Long senderId,
        String senderName,
        MessageType messageType,
        String content,
        Long fileId,
        String fileName,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        LocalDateTime deletedAt,
        long unreadCount
) {
}
