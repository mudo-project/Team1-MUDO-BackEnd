package com.academy.mudogroupware.workspace.domain.exception.workspace;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class WorkspaceNameConflictException extends ConflictException {

  public WorkspaceNameConflictException() {
    super(WorkspaceErrorCode.NAME_CONFLICT);
  }

  public WorkspaceNameConflictException(Throwable cause) {
    super(WorkspaceErrorCode.NAME_CONFLICT, WorkspaceErrorCode.NAME_CONFLICT.getMessage(), cause);
  }
}
