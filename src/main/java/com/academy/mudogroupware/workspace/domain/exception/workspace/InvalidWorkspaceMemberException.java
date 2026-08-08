package com.academy.mudogroupware.workspace.domain.exception.workspace;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class InvalidWorkspaceMemberException extends BadRequestException {

  public InvalidWorkspaceMemberException() {
    super(WorkspaceErrorCode.INVALID_MEMBER);
  }
}
