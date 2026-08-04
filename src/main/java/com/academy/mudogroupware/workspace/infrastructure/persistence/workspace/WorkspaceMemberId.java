package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMemberId implements Serializable {

  @Column(name = "workspace_id", nullable = false)
  private Long workspaceId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  private WorkspaceMemberId(Long workspaceId, Long userId) {
    this.workspaceId = workspaceId;
    this.userId = userId;
  }

  public static WorkspaceMemberId of(Long workspaceId, Long userId) {
    return new WorkspaceMemberId(workspaceId, userId);
  }
}
