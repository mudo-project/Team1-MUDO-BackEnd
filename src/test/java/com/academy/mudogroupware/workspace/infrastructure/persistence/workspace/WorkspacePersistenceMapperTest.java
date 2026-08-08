package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import org.junit.jupiter.api.Test;

class WorkspacePersistenceMapperTest {

  private final WorkspacePersistenceMapper mapper = new WorkspacePersistenceMapperImpl();

  @Test
  void preservesPersistedMembersWithoutAddingCreatorWhenMappingToDomain() {
    WorkspaceJpaEntity entity = WorkspaceJpaEntity.create(1L, "개발팀", 10L);
    entity.addMember(20L);

    Workspace workspace = mapper.toDomain(entity);

    assertThat(workspace.getMemberIds()).containsExactly(20L);
  }
}
