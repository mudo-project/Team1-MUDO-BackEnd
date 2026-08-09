package com.academy.mudogroupware.workspace.domain.exception.task;

import com.academy.mudogroupware.global.domain.common.exception.NotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;

public class RecurringTaskTemplateNotFoundException extends NotFoundException {

  public RecurringTaskTemplateNotFoundException() {
    super(WorkspaceErrorCode.RECURRING_TASK_TEMPLATE_NOT_FOUND);
  }
}
