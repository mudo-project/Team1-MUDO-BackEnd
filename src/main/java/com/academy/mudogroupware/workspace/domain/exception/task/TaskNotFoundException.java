package com.academy.mudogroupware.workspace.domain.exception.task;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class TaskNotFoundException extends NotFoundException {

  public TaskNotFoundException() {
    super(WorkspaceErrorCode.TASK_NOT_FOUND);
  }
}
