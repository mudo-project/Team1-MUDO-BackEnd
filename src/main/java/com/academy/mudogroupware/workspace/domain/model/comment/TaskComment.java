package com.academy.mudogroupware.workspace.domain.model.comment;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
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
        validateContent(content),
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
        validateContent(newContent),
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

  // 같은 사용자를 중복 멘션해도 멘션은 한 번만 남는다 — (comment_id, mentioned_user_id) 유니크
  // 제약과 일치시키기 위해 도메인에서 먼저 막는다. 순서는 입력 순서를 유지한다.
  private static List<TaskCommentMention> toMentions(List<Long> mentionedUserIds, LocalDateTime now) {
    return mentionedUserIds.stream()
        .distinct()
        .map(userId -> TaskCommentMention.create(userId, now))
        .toList();
  }

  // 댓글 내용은 항상 공백만으로 채워질 수 없다 — Request 계층의 @NotBlank가 API 경로를 막지만,
  // 도메인을 직접 호출하는 경로(다른 도메인 연동, 배치 등)도 동일하게 방어한다.
  private static String validateContent(String content) {
    if (content == null || content.trim().isEmpty()) {
      throw new BadRequestException();
    }
    return content.trim();
  }
}
