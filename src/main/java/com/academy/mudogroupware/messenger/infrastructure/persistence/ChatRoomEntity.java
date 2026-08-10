package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.academy.mudogroupware.messenger.domain.model.ChatRoomType;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatRoomType type;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "chat_room_member", joinColumns = @JoinColumn(name = "chat_room_id"))
    private List<ChatRoomMemberEmbeddable> members = new ArrayList<>();

    @Builder
    private ChatRoomEntity(Long id, String name, ChatRoomType type, Long createdBy,
                            LocalDateTime createdAt, List<ChatRoomMemberEmbeddable> members) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        if (members != null) {
            this.members = new ArrayList<>(members);
        }
    }
}
