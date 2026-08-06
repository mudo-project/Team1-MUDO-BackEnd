package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAlreadyActiveException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
        throw new WorkspaceNameConflictException(exception);
      }
      throw exception;
    }
  }

  // 활성화된 workspace 검색
  @Override
  public boolean existsByAcademyIdAndName(Long academyId, String name) {
    return workspaceJpaRepository.existsByAcademyIdAndNameAndDeletedAtIsNull(academyId, name);
  }

  @Override
  public Optional<Workspace> findById(Long workspaceId) {
    return workspaceJpaRepository.findActiveById(workspaceId)
        .map(workspacePersistenceMapper::toDomain);
  }

  @Override
  public Optional<Workspace> findByIdForUpdate(Long workspaceId) {
    return workspaceJpaRepository.findActiveByIdForUpdate(workspaceId)
        .map(workspacePersistenceMapper::toDomain);
  }

  @Override
  public void rename(Long workspaceId, String newName) {
    WorkspaceJpaEntity entity =
        workspaceJpaRepository.findById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);
    entity.rename(newName);
    try {
      workspaceJpaRepository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException exception) {
      if (isActiveNameConflict(exception)) {
        throw new WorkspaceNameConflictException(exception);
      }
      throw exception;
    }
  }

  @Override
  public void updateMembers(Long workspaceId, Set<Long> memberIds) {
    WorkspaceJpaEntity entity =
        workspaceJpaRepository.findById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);
    Set<Long> currentIds =
        entity.getMembers().stream()
            .map(WorkspaceMemberJpaEntity::getUserId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    memberIds.stream().filter(id -> !currentIds.contains(id)).forEach(entity::addMember);
    currentIds.stream().filter(id -> !memberIds.contains(id)).forEach(entity::removeMember);
    workspaceJpaRepository.saveAndFlush(entity);
  }

  @Override
  public void delete(Long workspaceId, LocalDateTime deletedAt) {
    WorkspaceJpaEntity entity =
        workspaceJpaRepository.findById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);
    entity.markDeleted(deletedAt);
    workspaceJpaRepository.saveAndFlush(entity);
  }

  @Override
  public Optional<Workspace> findDeletedByIdForUpdate(Long workspaceId) {
    Optional<WorkspaceJpaEntity> deletedEntity =
        workspaceJpaRepository.findDeletedByIdForUpdate(workspaceId);
    if (deletedEntity.isPresent()) {
      return deletedEntity.map(workspacePersistenceMapper::toDomain);
    }
    if (workspaceJpaRepository.existsById(workspaceId)) {
      throw new WorkspaceAlreadyActiveException();
    }
    return Optional.empty();
  }

  @Override
  public void recover(Long workspaceId, String finalName) {
    WorkspaceJpaEntity entity =
        workspaceJpaRepository.findById(workspaceId).orElseThrow(WorkspaceNotFoundException::new);
    entity.clearDeletedAt();
    entity.rename(finalName);
    try {
      workspaceJpaRepository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException exception) {
      if (isActiveNameConflict(exception)) {
        throw new WorkspaceNameConflictException(exception);
      }
      throw exception;
    }
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
