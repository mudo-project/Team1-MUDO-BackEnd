package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;

public class WorkspaceMemberNotFoundException extends NotFoundException {

  public WorkspaceMemberNotFoundException() {
    super(WorkspaceErrorCode.MEMBER_NOT_FOUND);
  }
}
