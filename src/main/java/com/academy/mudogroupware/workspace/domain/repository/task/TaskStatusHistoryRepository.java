package com.academy.mudogroupware.workspace.domain.repository.task;

import com.academy.mudogroupware.workspace.domain.model.task.TaskStatusHistory;

public interface TaskStatusHistoryRepository {

  void append(TaskStatusHistory history);
}
