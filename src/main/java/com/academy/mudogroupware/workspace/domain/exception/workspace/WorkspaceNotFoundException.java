package com.academy.mudogroupware.workspace.domain.exception.workspace;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class WorkspaceNotFoundException extends NotFoundException {

  public WorkspaceNotFoundException() {
    super(WorkspaceErrorCode.NOT_FOUND);
  }
}
