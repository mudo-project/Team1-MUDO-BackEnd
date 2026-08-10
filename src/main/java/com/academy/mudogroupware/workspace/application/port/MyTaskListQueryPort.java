package com.academy.mudogroupware.workspace.application.port;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.util.List;

public interface MyTaskListQueryPort {

  PageResult<MyTaskListItem> findMine(
      Long userId, List<TaskStatus> statuses, Long workspaceId, int page, int size);
}
