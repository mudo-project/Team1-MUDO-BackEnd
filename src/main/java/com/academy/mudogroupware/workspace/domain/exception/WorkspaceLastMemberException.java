package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class WorkspaceLastMemberException extends BadRequestException {

  public WorkspaceLastMemberException() {
    super(WorkspaceErrorCode.LAST_MEMBER_CANNOT_LEAVE);
  }
}
