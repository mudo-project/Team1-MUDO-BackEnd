package com.academy.mudogroupware.workspace.application.query;

import java.util.List;

public record WorkspaceDetail(
    Long workspaceId,
    String name,
    List<WorkspaceMemberInfo> members,
    List<WorkspaceTaskItem> tasks
) {

  public long memberCount() {
    return members.size();
  }

  public long taskCount() {
    return tasks.size();
  }
}
