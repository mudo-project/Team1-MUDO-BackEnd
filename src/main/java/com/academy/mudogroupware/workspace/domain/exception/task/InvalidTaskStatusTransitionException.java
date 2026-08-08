package com.academy.mudogroupware.workspace.domain.exception.task;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class InvalidTaskStatusTransitionException extends BadRequestException {

  public InvalidTaskStatusTransitionException() {
    super(WorkspaceErrorCode.INVALID_TASK_STATUS_TRANSITION);
  }
}
