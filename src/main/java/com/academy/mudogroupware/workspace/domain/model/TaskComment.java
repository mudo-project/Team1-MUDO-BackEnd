package com.academy.mudogroupware.workspace.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class TaskComment {

  private final Long id;
  private final Long taskId;
  private final Long authorId;
  private final String content;
  private final boolean completed;
  private final Long completedBy;
  private final LocalDateTime completedAt;
  private final List<TaskCommentMention> mentions;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  private TaskComment(
      Long id,
      Long taskId,
      Long authorId,
      String content,
      boolean completed,
      Long completedBy,
      LocalDateTime completedAt,
      List<TaskCommentMention> mentions,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.taskId = taskId;
    this.authorId = authorId;
    this.content = content;
    this.completed = completed;
    this.completedBy = completedBy;
    this.completedAt = completedAt;
    this.mentions = List.copyOf(mentions);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static TaskComment create(
      Long taskId, Long authorId, String content, List<Long> mentionedUserIds, LocalDateTime now) {
    return new TaskComment(
        null,
        taskId,
        authorId,
        content.trim(),
        false,
        null,
        null,
        toMentions(mentionedUserIds, now),
        now,
        now);
  }

  public static TaskComment restore(
      Long id,
      Long taskId,
      Long authorId,
      String content,
      boolean completed,
      Long completedBy,
      LocalDateTime completedAt,
      List<TaskCommentMention> mentions,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    return new TaskComment(
        id, taskId, authorId, content, completed, completedBy, completedAt, mentions, createdAt,
        updatedAt);
  }

  public TaskComment updateContent(String newContent, List<Long> newMentionedUserIds, LocalDateTime now) {
    return new TaskComment(
        id,
        taskId,
        authorId,
        newContent.trim(),
        completed,
        completedBy,
        completedAt,
        toMentions(newMentionedUserIds, now),
        createdAt,
        now);
  }

  public TaskComment toggleComplete(Long actorId, LocalDateTime now) {
    boolean newCompleted = !completed;
    return new TaskComment(
        id,
        taskId,
        authorId,
        content,
        newCompleted,
        newCompleted ? actorId : null,
        newCompleted ? now : null,
        mentions,
        createdAt,
        now);
  }

  public boolean belongsTo(Long targetTaskId) {
    return taskId.equals(targetTaskId);
  }

  private static List<TaskCommentMention> toMentions(List<Long> mentionedUserIds, LocalDateTime now) {
    return mentionedUserIds.stream().map(userId -> TaskCommentMention.create(userId, now)).toList();
  }
}
