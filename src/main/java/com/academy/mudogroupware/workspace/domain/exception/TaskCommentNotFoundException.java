package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class TaskCommentNotFoundException extends NotFoundException {

  public TaskCommentNotFoundException() {
    super(WorkspaceErrorCode.TASK_COMMENT_NOT_FOUND);
  }
}
