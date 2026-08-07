package com.academy.mudogroupware.workspace.domain.repository;

import com.academy.mudogroupware.workspace.domain.model.TaskStatusHistory;

public interface TaskStatusHistoryRepository {

  void append(TaskStatusHistory history);
}
