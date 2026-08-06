package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.DeleteWorkspaceCommand;

public interface DeleteWorkspaceUseCase {

  // 워크스페이스 소프트 삭제
  void delete(DeleteWorkspaceCommand command);
}
