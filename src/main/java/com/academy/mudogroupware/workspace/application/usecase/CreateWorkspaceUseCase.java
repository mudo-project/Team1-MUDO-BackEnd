package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.CreateWorkspaceCommand;

public interface CreateWorkspaceUseCase {

  // 워크스페이스 생성
  Long createWorkspace(CreateWorkspaceCommand command);
}
