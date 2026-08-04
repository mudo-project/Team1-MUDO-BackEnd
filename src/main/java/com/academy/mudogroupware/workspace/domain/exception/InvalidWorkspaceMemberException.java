package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidWorkspaceMemberException extends BadRequestException {

  public InvalidWorkspaceMemberException() {
    super(WorkspaceErrorCode.INVALID_MEMBER);
  }
}
