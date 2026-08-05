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
public class WorkspaceRecentAccessId implements Serializable {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "workspace_id", nullable = false)
  private Long workspaceId;

  private WorkspaceRecentAccessId(Long userId, Long workspaceId) {
    this.userId = userId;
    this.workspaceId = workspaceId;
  }

  public static WorkspaceRecentAccessId of(Long userId, Long workspaceId) {
    return new WorkspaceRecentAccessId(userId, workspaceId);
  }
}
