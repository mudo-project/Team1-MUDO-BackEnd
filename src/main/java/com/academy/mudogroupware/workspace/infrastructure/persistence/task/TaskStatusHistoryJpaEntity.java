package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_status_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class  TaskStatusHistoryJpaEntity extends CreatedAtEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "task_status_history_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false)
  private TaskJpaEntity task;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", nullable = true, length = 20)
  private TaskStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "current_status", nullable = false, length = 20)
  private TaskStatus currentStatus;

  @Column(name = "changed_by", nullable = true)
  private Long changedBy;

  private TaskStatusHistoryJpaEntity(
      TaskJpaEntity task,
      TaskStatus previousStatus,
      TaskStatus currentStatus,
      Long changedBy) {
    this.task = task;
    this.previousStatus = previousStatus;
    this.currentStatus = currentStatus;
    this.changedBy = changedBy;
  }

  public static TaskStatusHistoryJpaEntity systemChanged(
      TaskJpaEntity task, TaskStatus previousStatus, TaskStatus currentStatus) {
    return new TaskStatusHistoryJpaEntity(task, previousStatus, currentStatus, null);
  }

  public static TaskStatusHistoryJpaEntity userChanged(
      TaskJpaEntity task,
      TaskStatus previousStatus,
      TaskStatus currentStatus,
      Long changedBy) {
    return new TaskStatusHistoryJpaEntity(task, previousStatus, currentStatus, changedBy);
  }
}
