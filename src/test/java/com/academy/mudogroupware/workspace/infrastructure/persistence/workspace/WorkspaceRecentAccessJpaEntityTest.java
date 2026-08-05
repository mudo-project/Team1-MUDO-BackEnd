package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WorkspaceRecentAccessJpaEntityTest {

  private WorkspaceJpaEntity workspace;

  @BeforeEach
  void setUp() {
    workspace = WorkspaceJpaEntity.create(1L, "개발팀", 10L);
    ReflectionTestUtils.setField(workspace, "id", 1L);
  }

  @Test
  void updatesAccessedAtWithoutChangingCompositeKey() {
    LocalDateTime first = LocalDateTime.of(2026, 8, 5, 9, 0);
    LocalDateTime second = LocalDateTime.of(2026, 8, 5, 10, 0);

    WorkspaceRecentAccessJpaEntity access =
        WorkspaceRecentAccessJpaEntity.create(workspace, 10L, first);

    access.updateAccessedAt(second);

    assertThat(access.getUserId()).isEqualTo(10L);
    assertThat(access.getWorkspaceId()).isEqualTo(workspace.getId());
    assertThat(access.getLastAccessedAt()).isEqualTo(second);
  }
}
