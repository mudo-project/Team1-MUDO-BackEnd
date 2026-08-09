package com.academy.mudogroupware.workspace.domain.model.task;

import com.academy.mudogroupware.workspace.domain.exception.task.IllegalTaskDueAtException;
import com.academy.mudogroupware.workspace.domain.exception.task.InvalidTaskStatusTransitionException;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskDueAtRequiredException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class Task {

  private final Long id;
  private final Long workspaceId;
  private final Long recurringTemplateId;
  private final String title;
  private final TaskStatus status;
  private final LocalDate dueAt;
  private final LocalDateTime scheduledFor;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  private Task(
      Long id,
      Long workspaceId,
      Long recurringTemplateId,
      String title,
      TaskStatus status,
      LocalDate dueAt,
      LocalDateTime scheduledFor,
      Long createdBy,
      LocalDateTime createdAt) {
    this.id = id;
    this.workspaceId = workspaceId;
    this.recurringTemplateId = recurringTemplateId;
    this.title = title;
    this.status = status;
    this.dueAt = dueAt;
    this.scheduledFor = scheduledFor;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  // 일반 업무 생성. 마감일이 today 이전이면 최초 상태를 DELAYED로 둔다.
  public static Task create(
      Long workspaceId, String title, LocalDate dueAt, Long creatorId, LocalDate today) {
    TaskStatus initialStatus = dueAt.isBefore(today) ? TaskStatus.DELAYED : TaskStatus.WAITING;
    return new Task(
        null, workspaceId, null, title.trim(), initialStatus, dueAt, null, creatorId, null);
  }

  public static Task restore(
      Long id,
      Long workspaceId,
      Long recurringTemplateId,
      String title,
      TaskStatus status,
      LocalDate dueAt,
      LocalDateTime scheduledFor,
      Long createdBy) {
    return restore(
        id, workspaceId, recurringTemplateId, title, status, dueAt, scheduledFor, createdBy, null);
  }

  public static Task restore(
      Long id,
      Long workspaceId,
      Long recurringTemplateId,
      String title,
      TaskStatus status,
      LocalDate dueAt,
      LocalDateTime scheduledFor,
      Long createdBy,
      LocalDateTime createdAt) {
    return new Task(
        id, workspaceId, recurringTemplateId, title, status, dueAt, scheduledFor, createdBy,
        createdAt);
  }

  // newDueAt은 선택이다. 주어지면 규칙 2가 요구하지 않는 경우에도 마감일에 반영한다.
  public Task changeStatus(TaskStatus newStatus, LocalDate newDueAt, LocalDate today) {
    if (newDueAt != null && isRecurring()) {
      throw new IllegalTaskDueAtException();
    }
    if (newStatus == status) {
      // 실제 전환이 아니므로 규칙 1·2를 적용하지 않는다. 마감일만 반영한다.
      return newDueAt == null ? this : withDueAt(newDueAt);
    }
    if (newStatus == TaskStatus.DELAYED && status == TaskStatus.COMPLETED) {
      throw new InvalidTaskStatusTransitionException();
    }
    if (requiresNewDueAt(newStatus, today) && (newDueAt == null || newDueAt.isBefore(today))) {
      throw new TaskDueAtRequiredException();
    }
    LocalDate effectiveDueAt = newDueAt != null ? newDueAt : dueAt;
    return new Task(
        id, workspaceId, recurringTemplateId, title, newStatus, effectiveDueAt, scheduledFor,
        createdBy, createdAt);
  }

  public Task changeDueAt(LocalDate newDueAt) {
    if (isRecurring()) {
      throw new IllegalTaskDueAtException();
    }
    return withDueAt(newDueAt);
  }

  public boolean isRecurring() {
    return recurringTemplateId != null;
  }

  // 규칙 2: 기한이 지난 일반 업무를 미완료 상태로 되돌릴 때만 새 마감일이 필요하다.
  // 반복 업무는 due_at을 쓰지 않으므로 면제한다.
  private boolean requiresNewDueAt(TaskStatus newStatus, LocalDate today) {
    return isIncomplete(newStatus) && !isRecurring() && dueAt != null && dueAt.isBefore(today);
  }

  private boolean isIncomplete(TaskStatus target) {
    return target == TaskStatus.WAITING || target == TaskStatus.IN_PROGRESS;
  }

  private Task withDueAt(LocalDate newDueAt) {
    return new Task(
        id, workspaceId, recurringTemplateId, title, status, newDueAt, scheduledFor, createdBy,
        createdAt);
  }
}
