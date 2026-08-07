package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.CreateTaskCommand;

public interface CreateTaskUseCase {

  // 생성된 업무 번호를 반환한다.
  Long createTask(CreateTaskCommand command);
}
