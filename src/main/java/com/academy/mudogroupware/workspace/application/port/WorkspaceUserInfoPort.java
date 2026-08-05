package com.academy.mudogroupware.workspace.application.port;

import com.academy.mudogroupware.workspace.application.query.WorkspaceMemberInfo;
import java.util.List;
import java.util.Set;

public interface WorkspaceUserInfoPort {

  List<WorkspaceMemberInfo> findUserInfo(Set<Long> userIds);
}
