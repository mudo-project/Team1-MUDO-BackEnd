package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAlreadyActiveException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class WorkspacePersistenceAdapter implements WorkspaceRepository {

  private static final String ACTIVE_NAME_UNIQUE_CONSTRAINT = "uk_workspace_active_name";

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
  public boolean existsByName(String name) {
    return workspaceJpaRepository.existsByNameAndDeletedAtIsNull(name);
  }

  // 트랜잭션 없는 호출자(예: JwtChannelInterceptor의 워크스페이스 토픽 구독 인가)가 불러도
  // memberIds(lazy 컬렉션) 매핑까지 같은 세션에서 끝나도록 이 메서드 자체가 트랜잭션 경계를
  // 갖는다. 이게 없으면 findActiveById()의 조회 트랜잭션이 메서드 리턴과 함께 끝나버려서,
  // 그 다음 줄의 .map(toDomain)이 세션 밖에서 lazy 컬렉션을 읽다 LazyInitializationException을
  // 던진다(트랜잭션이 있는 Service 안에서 호출할 땐 그 트랜잭션에 편승해서 우연히 안 터졌음).
  @Override
  @Transactional(readOnly = true)
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
