package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class IllegalTaskDueAtException extends BadRequestException {

  public IllegalTaskDueAtException() {
    super(WorkspaceErrorCode.RECURRING_TASK_DUE_AT_NOT_ALLOWED);
  }
}
