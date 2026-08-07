package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;

public class InvalidMentionedUserException extends BadRequestException {

  public InvalidMentionedUserException() {
    super(
        WorkspaceErrorCode.INVALID_MENTIONED_USER, WorkspaceErrorCode.INVALID_MENTIONED_USER.getMessage());
  }
}
