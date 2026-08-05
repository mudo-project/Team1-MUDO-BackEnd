package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class WorkspaceNotFoundException extends NotFoundException {

  public WorkspaceNotFoundException() {
    super(WorkspaceErrorCode.NOT_FOUND);
  }
}
