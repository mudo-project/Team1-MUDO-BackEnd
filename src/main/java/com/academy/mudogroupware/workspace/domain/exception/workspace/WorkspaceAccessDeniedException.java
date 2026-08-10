package com.academy.mudogroupware.workspace.domain.exception.workspace;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class WorkspaceAccessDeniedException extends ForbiddenException {

  public WorkspaceAccessDeniedException() {
    super(WorkspaceErrorCode.ACCESS_DENIED, WorkspaceErrorCode.ACCESS_DENIED.getMessage());
  }
}
