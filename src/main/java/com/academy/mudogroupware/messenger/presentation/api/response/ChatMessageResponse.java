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
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from(ChatMessageView view) {
        return new ChatMessageResponse(view.id(), view.senderId(), view.senderName(), view.messageType(),
                view.content(), view.fileUrl(), view.fileName(), view.createdAt());
    }
}
