package com.academy.mudogroupware.workspace.domain.exception.comment;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class TaskCommentNotFoundException extends NotFoundException {

  public TaskCommentNotFoundException() {
    super(WorkspaceErrorCode.TASK_COMMENT_NOT_FOUND);
  }
}
