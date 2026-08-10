package com.academy.mudogroupware.workspace.domain.exception.workspace;

import com.academy.mudogroupware.global.domain.common.exception.ConflictException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class WorkspaceAlreadyActiveException extends ConflictException {

  public WorkspaceAlreadyActiveException() {
    super(WorkspaceErrorCode.ALREADY_ACTIVE);
  }
}
