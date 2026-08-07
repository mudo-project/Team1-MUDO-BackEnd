package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class TaskDueAtRequiredException extends BadRequestException {

  public TaskDueAtRequiredException() {
    super(WorkspaceErrorCode.TASK_DUE_AT_REQUIRED);
  }
}
