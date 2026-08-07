package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidTaskStatusTransitionException extends BadRequestException {

  public InvalidTaskStatusTransitionException() {
    super(WorkspaceErrorCode.INVALID_TASK_STATUS_TRANSITION);
  }
}
