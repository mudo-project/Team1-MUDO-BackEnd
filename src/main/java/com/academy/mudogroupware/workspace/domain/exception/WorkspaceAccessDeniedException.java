package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;

public class WorkspaceAccessDeniedException extends ForbiddenException {

  public WorkspaceAccessDeniedException() {
    super(WorkspaceErrorCode.ACCESS_DENIED, WorkspaceErrorCode.ACCESS_DENIED.getMessage());
  }
}
