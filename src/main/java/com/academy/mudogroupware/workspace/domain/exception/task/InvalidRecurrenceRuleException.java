package com.academy.mudogroupware.workspace.domain.exception.task;

import com.academy.mudogroupware.global.domain.common.exception.BadRequestException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class InvalidRecurrenceRuleException extends BadRequestException {

  public InvalidRecurrenceRuleException() {
    super(WorkspaceErrorCode.INVALID_RECURRENCE_RULE);
  }
}
