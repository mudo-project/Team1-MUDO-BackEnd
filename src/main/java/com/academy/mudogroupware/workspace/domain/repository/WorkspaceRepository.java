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
}
