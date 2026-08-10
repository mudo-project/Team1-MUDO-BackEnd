package com.academy.mudogroupware.workspace.domain.repository.workspace;

import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface WorkspaceRepository {

  Workspace save(Workspace workspace);

  boolean existsByName(String name);

  // 활성 워크스페이스를 락 없이 조회. 참여자 검증처럼 읽기만 필요한 경로에서 쓴다
  // (업무 생성·수정·삭제). 쓰기 경로는 findByIdForUpdate를 쓴다.
  Optional<Workspace> findById(Long workspaceId);

  // 활성 워크스페이스를 비관적 락으로 조회 (이름수정/삭제/참여자관리 공통 진입점)
  Optional<Workspace> findByIdForUpdate(Long workspaceId);

  void rename(Long workspaceId, String newName);

  // 대상 참여자 집합과 정확히 일치하도록 추가·제거를 함께 반영
  void updateMembers(Long workspaceId, Set<Long> memberIds);

  void delete(Long workspaceId, LocalDateTime deletedAt);

  // 삭제된 워크스페이스만 비관적 락으로 조회. 워크스페이스가 아예 없으면 empty, 있지만
  // 활성 상태면 WorkspaceAlreadyActiveException을 던진다.
  Optional<Workspace> findDeletedByIdForUpdate(Long workspaceId);

  // 선행 조건: 같은 트랜잭션 안에서 findDeletedByIdForUpdate로 잠금을 먼저 획득한 뒤 호출해야
  // 한다 — 어댑터 구현이 잠금 없는 재조회에 의존하기 때문이다(1차 캐시 히트로 안전성을 확보).
  // deletedAt을 초기화하고 이름을 finalName으로 반영한다. 활성 이름과 충돌하면
  // WorkspaceNameConflictException을 던진다.
  void recover(Long workspaceId, String finalName);
}
