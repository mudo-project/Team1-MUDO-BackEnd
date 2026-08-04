package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;

public class WorkspaceNameConflictException extends ConflictException {

  public WorkspaceNameConflictException() {
    super(WorkspaceErrorCode.NAME_CONFLICT);
  }
}
