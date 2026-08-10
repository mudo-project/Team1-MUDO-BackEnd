package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class WorkspacePersistenceAdapterTest {

  @Test
  void convertsActiveNameUniqueConstraintViolationToWorkspaceConflict() {
    WorkspaceJpaRepository jpaRepository =
        org.mockito.Mockito.mock(WorkspaceJpaRepository.class);
    WorkspacePersistenceMapper mapper =
        org.mockito.Mockito.mock(WorkspacePersistenceMapper.class);
    WorkspacePersistenceAdapter adapter = new WorkspacePersistenceAdapter(jpaRepository, mapper);
    Workspace workspace = workspace();
    when(mapper.toEntity(workspace)).thenReturn(WorkspaceJpaEntity.create("개발팀", 10L));
    DataIntegrityViolationException violation =
        new DataIntegrityViolationException(
            "Duplicate entry for key 'uk_workspace_active_name'");
    when(jpaRepository.saveAndFlush(any(WorkspaceJpaEntity.class))).thenThrow(violation);

    assertThatThrownBy(() -> adapter.save(workspace))
        .isInstanceOf(WorkspaceNameConflictException.class)
        .hasCause(violation);
  }

  @Test
  void preservesUnrelatedDataIntegrityViolation() {
    WorkspaceJpaRepository jpaRepository =
        org.mockito.Mockito.mock(WorkspaceJpaRepository.class);
    WorkspacePersistenceMapper mapper =
        org.mockito.Mockito.mock(WorkspacePersistenceMapper.class);
    WorkspacePersistenceAdapter adapter = new WorkspacePersistenceAdapter(jpaRepository, mapper);
    Workspace workspace = workspace();
    DataIntegrityViolationException violation =
        new DataIntegrityViolationException("foreign key violation");
    when(mapper.toEntity(workspace)).thenReturn(WorkspaceJpaEntity.create("개발팀", 10L));
    when(jpaRepository.saveAndFlush(any(WorkspaceJpaEntity.class))).thenThrow(violation);

    assertThatThrownBy(() -> adapter.save(workspace)).isSameAs(violation);
  }

  private Workspace workspace() {
    return Workspace.create("개발팀", 10L, Set.of());
  }
}
