package com.academy.mudogroupware.workspace.domain.exception.task;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class TaskDueAtRequiredException extends BadRequestException {

  public TaskDueAtRequiredException() {
    super(WorkspaceErrorCode.TASK_DUE_AT_REQUIRED);
  }
}
