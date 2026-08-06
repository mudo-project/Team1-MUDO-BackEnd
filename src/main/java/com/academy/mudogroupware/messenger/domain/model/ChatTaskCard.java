package com.academy.mudogroupware.messenger.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.academy.mudogroupware.messenger.domain.exception.MessengerErrorCode;
import com.academy.mudogroupware.messenger.domain.exception.MessengerException;

public final class ChatTaskCard {

    private final Long id;
    private final Long chatRoomId;
    private final Long assignerUserId;
    private String content;
    private LocalDate dueDate;
    private final List<ChatTaskAssignee> assignees;
    private final LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private ChatTaskCard(Long id, Long chatRoomId, Long assignerUserId, String content, LocalDate dueDate,
                          List<ChatTaskAssignee> assignees, LocalDateTime createdAt, LocalDateTime deletedAt) {
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
        this.deletedAt = deletedAt;
    }

    public static ChatTaskCard create(Long chatRoomId, Long assignerUserId, String content, LocalDate dueDate,
                                       List<Long> assigneeUserIds, LocalDateTime createdAt) {
        List<ChatTaskAssignee> assignees = toDistinctAssignees(assigneeUserIds);
        return new ChatTaskCard(null, chatRoomId, assignerUserId, content, dueDate, assignees, createdAt, null);
    }

    public static ChatTaskCard restore(Long id, Long chatRoomId, Long assignerUserId, String content,
                                        LocalDate dueDate, List<ChatTaskAssignee> assignees,
                                        LocalDateTime createdAt) {
        return restore(id, chatRoomId, assignerUserId, content, dueDate, assignees, createdAt, null);
    }

    public static ChatTaskCard restore(Long id, Long chatRoomId, Long assignerUserId, String content,
                                        LocalDate dueDate, List<ChatTaskAssignee> assignees,
                                        LocalDateTime createdAt, LocalDateTime deletedAt) {
        return new ChatTaskCard(id, chatRoomId, assignerUserId, content, dueDate, assignees, createdAt, deletedAt);
    }

    private static List<ChatTaskAssignee> toDistinctAssignees(List<Long> assigneeUserIds) {
        Set<Long> distinctAssigneeIds = assigneeUserIds == null
                ? Set.of()
                : assigneeUserIds.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctAssigneeIds.isEmpty()) {
            throw new MessengerException(MessengerErrorCode.ASSIGNEE_REQUIRED);
        }
        return distinctAssigneeIds.stream().map(ChatTaskAssignee::create).toList();
    }

    public void complete(Long userId, LocalDateTime completedAt) {
        ChatTaskAssignee assignee = findAssignee(userId)
                .orElseThrow(() -> new MessengerException(MessengerErrorCode.NOT_TASK_ASSIGNEE));
        assignee.complete(completedAt);
    }

    /**
     * 내용/마감일/담당자 전체를 새 값으로 교체한다. 유지되는 담당자는 완료 기록을 그대로 보존한다
     * (Repository가 이 결과를 반영할 때 유지되는 담당자 row는 건드리지 않고, 빠진 담당자만 삭제·추가된
     * 담당자만 삽입하는 방식으로 동시 완료 처리와의 유실을 방지한다).
     */
    public void update(Long requesterId, String content, LocalDate dueDate, List<Long> assigneeUserIds) {
        validateOwner(requesterId);
        validateNotDeleted();
        if (content == null || content.isBlank()) {
            throw new MessengerException(MessengerErrorCode.TASK_CONTENT_REQUIRED);
        }
        List<ChatTaskAssignee> newAssignees = toDistinctAssignees(assigneeUserIds);
        Map<Long, ChatTaskAssignee> existingByUserId = this.assignees.stream()
                .collect(Collectors.toMap(ChatTaskAssignee::getUserId, assignee -> assignee));

        this.content = content;
        this.dueDate = dueDate;
        this.assignees.clear();
        for (ChatTaskAssignee newAssignee : newAssignees) {
            this.assignees.add(existingByUserId.getOrDefault(newAssignee.getUserId(), newAssignee));
        }
    }

    public void delete(Long requesterId, LocalDateTime deletedAt) {
        validateOwner(requesterId);
        if (this.deletedAt == null) {
            this.deletedAt = deletedAt;
        }
    }

    private void validateOwner(Long requesterId) {
        if (!assignerUserId.equals(requesterId)) {
            throw new MessengerException(MessengerErrorCode.NOT_TASK_CARD_OWNER);
        }
    }

    private void validateNotDeleted() {
        if (isDeleted()) {
            throw new MessengerException(MessengerErrorCode.TASK_CARD_ALREADY_DELETED);
        }
    }

    public boolean isDeleted() {
        return deletedAt != null;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
