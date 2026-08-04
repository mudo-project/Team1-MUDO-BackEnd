package com.academy.mudogroupware.messenger.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;

public final class ChatTaskCard {

    private final Long id;
    private final Long chatRoomId;
    private final Long assignerUserId;
    private final String content;
    private final LocalDate dueDate;
    private final List<ChatTaskAssignee> assignees;
    private final LocalDateTime createdAt;

    private ChatTaskCard(Long id, Long chatRoomId, Long assignerUserId, String content, LocalDate dueDate,
                          List<ChatTaskAssignee> assignees, LocalDateTime createdAt) {
        if (chatRoomId == null) {
            throw new IllegalArgumentException("chatRoomId must not be null");
        }
        if (assignerUserId == null) {
            throw new IllegalArgumentException("assignerUserId must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new MessengerException(MessengerErrorCode.TASK_CONTENT_REQUIRED);
        }
        if (assignees == null || assignees.isEmpty()) {
            throw new MessengerException(MessengerErrorCode.ASSIGNEE_REQUIRED);
        }
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.assignerUserId = assignerUserId;
        this.content = content;
        this.dueDate = dueDate;
        this.assignees = new ArrayList<>(assignees);
        this.createdAt = createdAt;
    }

    public static ChatTaskCard create(Long chatRoomId, Long assignerUserId, String content, LocalDate dueDate,
                                       List<Long> assigneeUserIds) {
        if (assigneeUserIds == null || assigneeUserIds.isEmpty()) {
            throw new MessengerException(MessengerErrorCode.ASSIGNEE_REQUIRED);
        }
        Set<Long> distinctAssigneeIds = new LinkedHashSet<>(assigneeUserIds);
        List<ChatTaskAssignee> assignees = distinctAssigneeIds.stream().map(ChatTaskAssignee::create).toList();

        return new ChatTaskCard(null, chatRoomId, assignerUserId, content, dueDate, assignees,
                LocalDateTime.now());
    }

    public static ChatTaskCard restore(Long id, Long chatRoomId, Long assignerUserId, String content,
                                        LocalDate dueDate, List<ChatTaskAssignee> assignees,
                                        LocalDateTime createdAt) {
        return new ChatTaskCard(id, chatRoomId, assignerUserId, content, dueDate, assignees, createdAt);
    }

    public void complete(Long userId, LocalDateTime completedAt) {
        ChatTaskAssignee assignee = findAssignee(userId)
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.NOT_TASK_ASSIGNEE));
        assignee.complete(completedAt);
    }

    public boolean isAssignee(Long userId) {
        return findAssignee(userId).isPresent();
    }

    private Optional<ChatTaskAssignee> findAssignee(Long userId) {
        return assignees.stream().filter(assignee -> assignee.getUserId().equals(userId)).findFirst();
    }

    public long getCompletedCount() {
        return assignees.stream().filter(ChatTaskAssignee::isCompleted).count();
    }

    public int getAssigneeCount() {
        return assignees.size();
    }

    public boolean isFullyCompleted() {
        return getCompletedCount() == getAssigneeCount();
    }

    public Long getId() {
        return id;
    }

    public Long getChatRoomId() {
        return chatRoomId;
    }

    public Long getAssignerUserId() {
        return assignerUserId;
    }

    public String getContent() {
        return content;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public List<ChatTaskAssignee> getAssignees() {
        return Collections.unmodifiableList(assignees);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
