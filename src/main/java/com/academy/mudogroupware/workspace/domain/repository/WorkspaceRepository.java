package com.academy.mudogroupware.workspace.domain.repository;

import com.academy.mudogroupware.workspace.domain.model.Workspace;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface WorkspaceRepository {

  Workspace save(Workspace workspace);

  boolean existsByAcademyIdAndName(Long academyId, String name);

  // 활성 워크스페이스를 비관적 락으로 조회 (이름수정/삭제/참여자관리 공통 진입점)
  Optional<Workspace> findByIdForUpdate(Long workspaceId);

  void rename(Long workspaceId, String newName);

  // 대상 참여자 집합과 정확히 일치하도록 추가·제거를 함께 반영
  void updateMembers(Long workspaceId, Set<Long> memberIds);

  void delete(Long workspaceId, LocalDateTime deletedAt);

  // 삭제된 워크스페이스만 비관적 락으로 조회. 워크스페이스가 아예 없으면 empty, 있지만
  // 활성 상태면 WorkspaceAlreadyActiveException을 던진다.
  Optional<Workspace> findDeletedByIdForUpdate(Long workspaceId);

  // deletedAt을 초기화하고 이름을 finalName으로 반영한다. 활성 이름과 충돌하면
  // WorkspaceNameConflictException을 던진다.
  void recover(Long workspaceId, String finalName);
}
