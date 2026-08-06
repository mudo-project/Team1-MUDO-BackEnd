package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.RecoverWorkspaceCommand;

public interface RecoverWorkspaceUseCase {

  // 소프트 삭제된 워크스페이스를 복구하고, 최종 반영된 이름(충돌 시 타임스탬프 접미사 포함)을 반환한다.
  String recover(RecoverWorkspaceCommand command);
}
