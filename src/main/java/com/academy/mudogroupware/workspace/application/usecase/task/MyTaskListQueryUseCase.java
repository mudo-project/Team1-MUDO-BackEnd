package com.academy.mudogroupware.workspace.application.usecase.task;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;

public interface MyTaskListQueryUseCase {

  PageResult<MyTaskListItem> getMyTasks(
      Long requesterId, TaskStatus status, Long workspaceId, int page, int size);
}
