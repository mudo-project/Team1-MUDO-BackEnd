package com.academy.mudogroupware.workspace.application.usecase.workspace;

import com.academy.mudogroupware.workspace.application.command.workspace.AddWorkspaceMembersCommand;
import java.util.Set;

public interface AddWorkspaceMembersUseCase {

  // 참여자 추가, 새로 추가된 사용자 id만 반환 (이미 참여 중인 사용자는 멱등 처리로 제외)
  Set<Long> addMembers(AddWorkspaceMembersCommand command);
}
