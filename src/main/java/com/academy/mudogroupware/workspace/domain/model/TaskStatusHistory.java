package com.academy.mudogroupware.workspace.domain.model;

import lombok.Getter;

@Getter
public class TaskStatusHistory {

  private final Long taskId;
  private final TaskStatus previousStatus;
  private final TaskStatus currentStatus;
  private final Long changedBy;

  private TaskStatusHistory(
      Long taskId, TaskStatus previousStatus, TaskStatus currentStatus, Long changedBy) {
    this.taskId = taskId;
    this.previousStatus = previousStatus;
    this.currentStatus = currentStatus;
    this.changedBy = changedBy;
  }

  // 사용자가 직접 변경한 이력. 최초 생성 이력은 previousStatus를 null로 넘긴다.
  public static TaskStatusHistory userChanged(
      Long taskId, TaskStatus previousStatus, TaskStatus currentStatus, Long changedBy) {
    return new TaskStatusHistory(taskId, previousStatus, currentStatus, changedBy);
  }

  // 스케줄러가 자동 처리한 이력. changedBy = null이 시스템 처리자를 의미한다.
  public static TaskStatusHistory systemChanged(
      Long taskId, TaskStatus previousStatus, TaskStatus currentStatus) {
    return new TaskStatusHistory(taskId, previousStatus, currentStatus, null);
  }
}
