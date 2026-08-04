package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WorkspaceRecentAccessAdapterTest {

  @Test
  void updatesExistingRecentAccess() {
    WorkspaceRecentAccessJpaRepository accessRepository =
        org.mockito.Mockito.mock(WorkspaceRecentAccessJpaRepository.class);
    WorkspaceJpaRepository workspaceRepository = org.mockito.Mockito.mock(WorkspaceJpaRepository.class);
    WorkspaceRecentAccessAdapter adapter =
        new WorkspaceRecentAccessAdapter(accessRepository, workspaceRepository);
    WorkspaceRecentAccessId id = WorkspaceRecentAccessId.of(10L, 100L);
    WorkspaceRecentAccessJpaEntity entity =
        WorkspaceRecentAccessJpaEntity.create(
            WorkspaceJpaEntity.create(1L, "Workspace", 10L), 10L, LocalDateTime.MIN);
    LocalDateTime accessedAt = LocalDateTime.of(2026, 8, 5, 10, 0);
    when(accessRepository.findById(id)).thenReturn(Optional.of(entity));

    adapter.upsert(10L, 100L, accessedAt);

    assertThat(entity.getLastAccessedAt()).isEqualTo(accessedAt);
    verify(accessRepository, never()).save(any());
    verify(workspaceRepository, never()).getReferenceById(any());
  }

  @Test
  void createsRecentAccessWithWorkspaceReferenceWhenMissing() {
    WorkspaceRecentAccessJpaRepository accessRepository =
        org.mockito.Mockito.mock(WorkspaceRecentAccessJpaRepository.class);
    WorkspaceJpaRepository workspaceRepository = org.mockito.Mockito.mock(WorkspaceJpaRepository.class);
    WorkspaceRecentAccessAdapter adapter =
        new WorkspaceRecentAccessAdapter(accessRepository, workspaceRepository);
    WorkspaceRecentAccessId id = WorkspaceRecentAccessId.of(10L, 100L);
    WorkspaceJpaEntity workspace = org.mockito.Mockito.mock(WorkspaceJpaEntity.class);
    LocalDateTime accessedAt = LocalDateTime.of(2026, 8, 5, 10, 0);
    when(accessRepository.findById(id)).thenReturn(Optional.empty());
    when(workspaceRepository.getReferenceById(100L)).thenReturn(workspace);
    when(workspace.getId()).thenReturn(100L);

    adapter.upsert(10L, 100L, accessedAt);

    org.mockito.ArgumentCaptor<WorkspaceRecentAccessJpaEntity> captor =
        org.mockito.ArgumentCaptor.forClass(WorkspaceRecentAccessJpaEntity.class);
    verify(accessRepository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(10L);
    assertThat(captor.getValue().getWorkspaceId()).isEqualTo(100L);
    assertThat(captor.getValue().getLastAccessedAt()).isEqualTo(accessedAt);
  }
}
