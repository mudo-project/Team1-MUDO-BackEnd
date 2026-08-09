package com.academy.mudogroupware.workspace.application.usecase.task;

import com.academy.mudogroupware.workspace.application.command.task.CreateRecurringTaskTemplateCommand;

public interface CreateRecurringTaskTemplateUseCase {

  // 생성된 템플릿 번호를 반환한다.
  Long create(CreateRecurringTaskTemplateCommand command);
}
