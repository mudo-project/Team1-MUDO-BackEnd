package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskCommentJpaEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "comment_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false)
  private TaskJpaEntity task;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "is_completed", nullable = false)
  private boolean completed;

  @Column(name = "completed_by")
  private Long completedBy;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "author_id", nullable = false, updatable = false)
  private Long authorId;

  private TaskCommentJpaEntity(TaskJpaEntity task, Long authorId, String content) {
    this.task = task;
    this.authorId = authorId;
    this.content = content;
    this.completed = false;
  }

  public static TaskCommentJpaEntity create(TaskJpaEntity task, Long authorId, String content) {
    return new TaskCommentJpaEntity(task, authorId, content);
  }

  public void complete(Long completedBy, LocalDateTime completedAt) {
    if (completed) {
      return;
    }
    this.completed = true;
    this.completedBy = completedBy;
    this.completedAt = completedAt;
  }

  public void cancelCompletion() {
    if (!completed) {
      return;
    }
    this.completed = false;
    this.completedBy = null;
    this.completedAt = null;
  }

  public void updateContent(String content) {
    this.content = content;
  }
}
