package com.academy.mudogroupware.messenger.domain.model;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;

public final class ChatMessage {

    private final Long id;
    private final Long chatRoomId;
    private final Long senderUserId;
    private final MessageType messageType;
    private final String content;
    private final String fileUrl;
    private final String fileName;
    private final LocalDateTime createdAt;

    private ChatMessage(Long id, Long chatRoomId, Long senderUserId, MessageType messageType, String content,
                         String fileUrl, String fileName, LocalDateTime createdAt) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("chatRoomId must not be null");
        }
        if (senderUserId == null) {
            throw new IllegalArgumentException("senderUserId must not be null");
        }
        if (messageType == null) {
            throw new IllegalArgumentException("messageType must not be null");
        }
        if (messageType == MessageType.TEXT) {
            if (content == null || content.isBlank()) {
                throw new MessengerException(MessengerErrorCode.MESSAGE_CONTENT_REQUIRED);
            }
        } else if (fileUrl == null || fileUrl.isBlank()) {
            throw new MessengerException(MessengerErrorCode.FILE_URL_REQUIRED);
        }
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderUserId = senderUserId;
        this.messageType = messageType;
        this.content = content;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.createdAt = createdAt;
    }

    public static ChatMessage create(Long chatRoomId, Long senderUserId, MessageType messageType, String content,
                                      String fileUrl, String fileName) {
        return new ChatMessage(null, chatRoomId, senderUserId, messageType, content, fileUrl, fileName,
                LocalDateTime.now());
    }

    public static ChatMessage restore(Long id, Long chatRoomId, Long senderUserId, MessageType messageType,
                                       String content, String fileUrl, String fileName, LocalDateTime createdAt) {
        return new ChatMessage(id, chatRoomId, senderUserId, messageType, content, fileUrl, fileName, createdAt);
    }

    public Long getId() {
        return id;
    }

    public Long getChatRoomId() {
        return chatRoomId;
    }

    public Long getSenderUserId() {
        return senderUserId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
