package com.academy.mudogroupware.workspace.application.usecase.task;

import com.academy.mudogroupware.workspace.application.query.task.TaskDetail;

public interface TaskDetailQueryUseCase {

  TaskDetail getTaskDetail(Long workspaceId, Long taskId, Long requesterId);
}
