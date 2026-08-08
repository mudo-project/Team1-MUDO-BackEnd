package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "task_comment_mention",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_task_comment_mention_comment_user",
            columnNames = {"comment_id", "mentioned_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskCommentMentionJpaEntity extends CreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "comment_mention_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "comment_id", nullable = false)
  private TaskCommentJpaEntity comment;

  @Column(name = "mentioned_user_id", nullable = false)
  private Long mentionedUserId;

  private TaskCommentMentionJpaEntity(TaskCommentJpaEntity comment, Long mentionedUserId) {
    this.comment = comment;
    this.mentionedUserId = mentionedUserId;
  }

  public static TaskCommentMentionJpaEntity create(
      TaskCommentJpaEntity comment, Long mentionedUserId) {
    return new TaskCommentMentionJpaEntity(comment, mentionedUserId);
  }
}
