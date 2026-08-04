package com.academy.mudogroupware.workspace.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

  @Test
  void includesCreatorInMembersWhenCreated() {
    Workspace workspace = Workspace.builder()
        .academyId(1L)
        .name("개발팀")
        .createdBy(10L)
        .memberIds(Set.of(20L))
        .build();

    assertThat(workspace.getMemberIds()).containsExactlyInAnyOrder(10L, 20L);
  }
}
