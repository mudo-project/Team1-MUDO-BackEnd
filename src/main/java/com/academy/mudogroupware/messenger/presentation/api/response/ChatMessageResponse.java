package com.academy.mudogroupware.messenger.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.application.query.ChatMessageView;
import com.academy.mudogroupware.messenger.domain.model.MessageType;

public record ChatMessageResponse(
        Long id,
        Long senderId,
        String senderName,
        MessageType messageType,
        String content,
        String fileUrl,
        String fileName,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        LocalDateTime deletedAt,
        boolean deleted,
        long unreadCount
) {

    public static ChatMessageResponse from(ChatMessageView view) {
        boolean deleted = view.deletedAt() != null;
        return new ChatMessageResponse(view.id(), view.senderId(), view.senderName(), view.messageType(),
                deleted ? null : view.content(), deleted ? null : view.fileUrl(), deleted ? null : view.fileName(),
                view.createdAt(), view.editedAt(), view.deletedAt(), deleted, view.unreadCount());
    }
}
