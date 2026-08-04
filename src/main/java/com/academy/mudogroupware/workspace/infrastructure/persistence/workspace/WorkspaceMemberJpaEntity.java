package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspace_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMemberJpaEntity extends CreatedAtEntity {

  @EmbeddedId
  private WorkspaceMemberId id;

  @MapsId("workspaceId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id", nullable = false)
  private WorkspaceJpaEntity workspace;

  private WorkspaceMemberJpaEntity(WorkspaceJpaEntity workspace, Long userId) {
    this.id = WorkspaceMemberId.of(workspace.getId(), userId);
    this.workspace = workspace;
  }

  public static WorkspaceMemberJpaEntity create(WorkspaceJpaEntity workspace, Long userId) {
    return new WorkspaceMemberJpaEntity(workspace, userId);
  }

  public Long getUserId() {
    return id.getUserId();
  }
}
