package com.academy.mudogroupware.messenger.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_task_card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatTaskCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "assigner_user_id", nullable = false)
    private Long assignerUserId;

    @Lob
    private String content;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "chat_task_assignee", joinColumns = @JoinColumn(name = "card_id"))
    private List<ChatTaskAssigneeEmbeddable> assignees = new ArrayList<>();

    @Builder
    private ChatTaskCardEntity(Long id, Long chatRoomId, Long assignerUserId, String content, LocalDate dueDate,
                                LocalDateTime createdAt, List<ChatTaskAssigneeEmbeddable> assignees) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.assignerUserId = assignerUserId;
        this.content = content;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        if (assignees != null) {
            this.assignees = new ArrayList<>(assignees);
        }
    }
}
