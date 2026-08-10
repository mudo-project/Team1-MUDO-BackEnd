package com.academy.mudogroupware.workspace.application.usecase.comment;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentListItem;

public interface TaskCommentListQueryUseCase {

  PageResult<TaskCommentListItem> getComments(
      Long workspaceId,
      Long taskId,
      Long requesterId,
      int page,
      int size,
      Long academyId,
      boolean canReadAll);
}
