package com.academy.mudogroupware.workspace.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

  @Test
  void includesCreatorInMembersWhenCreated() {
    Workspace workspace = Workspace.create(1L, "개발팀", 10L, Set.of(20L));

    assertThat(workspace.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void removesCreatorDuplicateWhenCreated() {
    Workspace workspace = Workspace.create(1L, "개발팀", 10L, Set.of(10L, 20L));

    assertThat(workspace.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void preservesStoredMembersWithoutAddingCreatorWhenRestored() {
    Workspace workspace = Workspace.restore(1L, 1L, "개발팀", 10L, Set.of(20L));

    assertThat(workspace.getMemberIds()).containsExactly(20L);
  }

  @Test
  void defensivelyCopiesAdditionalMembersWhenCreated() {
    Set<Long> additionalMemberIds = new LinkedHashSet<>(Set.of(20L));

    Workspace workspace = Workspace.create(1L, "개발팀", 10L, additionalMemberIds);
    additionalMemberIds.add(30L);

    assertThat(workspace.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }
}
