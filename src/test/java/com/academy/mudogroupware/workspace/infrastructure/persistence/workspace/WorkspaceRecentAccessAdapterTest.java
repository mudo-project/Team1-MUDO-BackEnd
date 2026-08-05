package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WorkspaceRecentAccessAdapterTest {

  @Test
  void delegatesRecentAccessUpsertToRepository() {
    WorkspaceRecentAccessJpaRepository accessRepository =
        org.mockito.Mockito.mock(WorkspaceRecentAccessJpaRepository.class);
    WorkspaceRecentAccessAdapter adapter = new WorkspaceRecentAccessAdapter(accessRepository);
    LocalDateTime accessedAt = LocalDateTime.of(2026, 8, 5, 10, 0);

    adapter.upsert(10L, 100L, accessedAt);

    verify(accessRepository).upsert(10L, 100L, accessedAt);
  }
}
