package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspace_recent_access")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceRecentAccessJpaEntity {

  @EmbeddedId
  private WorkspaceRecentAccessId id;

  @MapsId("workspaceId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id", nullable = false)
  private WorkspaceJpaEntity workspace;

  @Column(name = "last_accessed_at", nullable = false)
  private LocalDateTime lastAccessedAt;

  private WorkspaceRecentAccessJpaEntity(
      WorkspaceJpaEntity workspace, Long userId, LocalDateTime accessedAt) {
    this.id = WorkspaceRecentAccessId.of(userId, workspace.getId());
    this.workspace = workspace;
    this.lastAccessedAt = accessedAt;
  }

  public static WorkspaceRecentAccessJpaEntity create(
      WorkspaceJpaEntity workspace, Long userId, LocalDateTime accessedAt) {
    return new WorkspaceRecentAccessJpaEntity(workspace, userId, accessedAt);
  }

  public void updateAccessedAt(LocalDateTime accessedAt) {
    this.lastAccessedAt = accessedAt;
  }

  public Long getUserId() {
    return id.getUserId();
  }

  public Long getWorkspaceId() {
    return id.getWorkspaceId();
  }
}
