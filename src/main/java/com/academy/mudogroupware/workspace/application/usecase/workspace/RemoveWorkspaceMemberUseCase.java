package com.academy.mudogroupware.workspace.application.usecase.workspace;

import com.academy.mudogroupware.workspace.application.command.workspace.RemoveWorkspaceMemberCommand;

public interface RemoveWorkspaceMemberUseCase {

  // 참여자 제거. 요청자와 대상이 같으면 자진 탈퇴로 동작한다.
  void removeMember(RemoveWorkspaceMemberCommand command);
}
