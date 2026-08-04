package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "task",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_task_recurring_template_scheduled_for",
            columnNames = {"recurring_template_id", "scheduled_for"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskJpaEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "task_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id", nullable = false)
  private WorkspaceJpaEntity workspace;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "recurring_template_id", nullable = true)
  private RecurringTaskTemplateJpaEntity recurringTemplate;

  @Column(nullable = false, length = 200)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskStatus status;

  @Column(name = "due_at")
  private LocalDateTime dueAt;

  @Column(name = "scheduled_for")
  private LocalDateTime scheduledFor;

  @Column(name = "created_by", nullable = false, updatable = false)
  private Long createdBy;

  private TaskJpaEntity(
      WorkspaceJpaEntity workspace,
      RecurringTaskTemplateJpaEntity recurringTemplate,
      String title,
      Long createdBy,
      TaskStatus status,
      LocalDateTime dueAt,
      LocalDateTime scheduledFor) {
    this.workspace = workspace;
    this.recurringTemplate = recurringTemplate;
    this.title = title;
    this.createdBy = createdBy;
    this.status = status;
    this.dueAt = dueAt;
    this.scheduledFor = scheduledFor;
  }

  public static TaskJpaEntity create(
      WorkspaceJpaEntity workspace,
      RecurringTaskTemplateJpaEntity recurringTemplate,
      String title,
      Long createdBy,
      TaskStatus status,
      LocalDateTime dueAt,
      LocalDateTime scheduledFor) {
    return new TaskJpaEntity(
        workspace, recurringTemplate, title, createdBy, status, dueAt, scheduledFor);
  }
}
