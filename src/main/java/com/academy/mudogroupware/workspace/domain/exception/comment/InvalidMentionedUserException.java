package com.academy.mudogroupware.workspace.domain.exception.comment;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class InvalidMentionedUserException extends BadRequestException {

  public InvalidMentionedUserException() {
    super(
        WorkspaceErrorCode.INVALID_MENTIONED_USER, WorkspaceErrorCode.INVALID_MENTIONED_USER.getMessage());
  }
}
