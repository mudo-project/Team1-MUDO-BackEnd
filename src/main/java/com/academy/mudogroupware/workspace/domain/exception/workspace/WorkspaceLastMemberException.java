package com.academy.mudogroupware.workspace.domain.exception.workspace;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class WorkspaceLastMemberException extends BadRequestException {

  public WorkspaceLastMemberException() {
    super(WorkspaceErrorCode.LAST_MEMBER_CANNOT_LEAVE);
  }
}
