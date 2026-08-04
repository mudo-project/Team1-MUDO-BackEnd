package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNameConflictException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorkspacePersistenceAdapter implements WorkspaceRepository {

  private static final String ACTIVE_NAME_UNIQUE_CONSTRAINT =
      "uk_workspace_academy_active_name";

  private final WorkspaceJpaRepository workspaceJpaRepository;
  private final WorkspacePersistenceMapper workspacePersistenceMapper;

  @Override
  public Workspace save(Workspace workspace) {
    WorkspaceJpaEntity entity = workspacePersistenceMapper.toEntity(workspace);
    workspace.getMemberIds().forEach(entity::addMember);

    try {
      return workspacePersistenceMapper.toDomain(workspaceJpaRepository.saveAndFlush(entity));
    } catch (DataIntegrityViolationException exception) {
      if (isActiveNameConflict(exception)) {
        throw new WorkspaceNameConflictException();
      }
      throw exception;
    }
  }

  @Override
  public boolean existsByAcademyIdAndName(Long academyId, String name) {
    return workspaceJpaRepository.existsByAcademyIdAndNameAndDeletedAtIsNull(academyId, name);
  }

  private boolean isActiveNameConflict(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      String message = current.getMessage();
      if (message != null
          && message.toLowerCase(Locale.ROOT).contains(ACTIVE_NAME_UNIQUE_CONSTRAINT)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
