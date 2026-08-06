package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class TaskNotFoundException extends NotFoundException {

  public TaskNotFoundException() {
    super(WorkspaceErrorCode.TASK_NOT_FOUND);
  }
}
