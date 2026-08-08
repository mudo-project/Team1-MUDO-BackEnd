package com.academy.mudogroupware.workspace.application.usecase.workspace;

public interface RecordWorkspaceRecentAccessUseCase {

  void recordRecentAccess(Long academyId, Long userId, Long workspaceId, boolean canReadAll);
}
