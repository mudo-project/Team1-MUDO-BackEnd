package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.RenameWorkspaceCommand;

public interface RenameWorkspaceUseCase {

  // 워크스페이스 이름 수정, 변경된 이름 반환
  String rename(RenameWorkspaceCommand command);
}
