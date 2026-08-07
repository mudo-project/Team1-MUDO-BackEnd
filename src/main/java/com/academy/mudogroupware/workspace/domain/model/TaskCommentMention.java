package com.academy.mudogroupware.workspace.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class TaskCommentMention {

  private final Long id;
  private final Long mentionedUserId;
  private final LocalDateTime createdAt;

  private TaskCommentMention(Long id, Long mentionedUserId, LocalDateTime createdAt) {
    this.id = id;
    this.mentionedUserId = mentionedUserId;
    this.createdAt = createdAt;
  }

  public static TaskCommentMention create(Long mentionedUserId, LocalDateTime now) {
    return new TaskCommentMention(null, mentionedUserId, now);
  }

  public static TaskCommentMention restore(Long id, Long mentionedUserId, LocalDateTime createdAt) {
    return new TaskCommentMention(id, mentionedUserId, createdAt);
  }
}
