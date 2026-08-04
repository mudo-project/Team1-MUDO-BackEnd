package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.messenger.domain.model.MessageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 10)
    private MessageType messageType;

    @Lob
    private String content;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_name", length = 200)
    private String fileName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ChatMessageEntity(Long id, Long chatRoomId, Long senderUserId, MessageType messageType, String content,
                               String fileUrl, String fileName, LocalDateTime createdAt) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderUserId = senderUserId;
        this.messageType = messageType;
        this.content = content;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
        this.createdAt = createdAt;
    }
}
