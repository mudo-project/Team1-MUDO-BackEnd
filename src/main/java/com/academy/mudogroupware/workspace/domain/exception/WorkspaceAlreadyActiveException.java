package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class WorkspaceAlreadyActiveException extends ConflictException {

  public WorkspaceAlreadyActiveException() {
    super(WorkspaceErrorCode.ALREADY_ACTIVE);
  }
}
