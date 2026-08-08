package com.academy.mudogroupware.workspace.domain.exception.workspace;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class WorkspaceMemberNotFoundException extends NotFoundException {

  public WorkspaceMemberNotFoundException() {
    super(WorkspaceErrorCode.MEMBER_NOT_FOUND);
  }
}
