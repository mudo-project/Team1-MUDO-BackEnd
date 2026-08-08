package com.academy.mudogroupware.workspace.domain.exception.task;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class IllegalTaskDueAtException extends BadRequestException {

  public IllegalTaskDueAtException() {
    super(WorkspaceErrorCode.RECURRING_TASK_DUE_AT_NOT_ALLOWED);
  }
}
