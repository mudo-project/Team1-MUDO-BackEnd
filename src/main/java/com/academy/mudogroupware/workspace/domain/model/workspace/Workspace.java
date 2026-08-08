package com.academy.mudogroupware.workspace.domain.model.workspace;

import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceLastMemberException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceMemberNotFoundException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;

@Getter
public class Workspace {

  private final Long id;
  private final Long academyId;
  private final String name;
  private final Long createdBy;
  private final Set<Long> memberIds;

  private Workspace(Long id, Long academyId, String name, Long createdBy, Set<Long> memberIds) {
    this.id = id;
    this.academyId = academyId;
    this.name = name;
    this.createdBy = createdBy;
    this.memberIds = Collections.unmodifiableSet(new LinkedHashSet<>(memberIds));
  }

  public static Workspace create(
      Long academyId, String name, Long creatorId, Set<Long> additionalMemberIds) {
    Set<Long> memberIds = new LinkedHashSet<>(additionalMemberIds);
    memberIds.add(creatorId);
    return new Workspace(null, academyId, name, creatorId, memberIds);
  }

  public static Workspace restore(
      Long id, Long academyId, String name, Long createdBy, Set<Long> memberIds) {
    return new Workspace(id, academyId, name, createdBy, memberIds);
  }

  public Workspace rename(String newName) {
    return new Workspace(id, academyId, newName.trim(), createdBy, memberIds);
  }

  public Workspace addMembers(Set<Long> candidateIds) {
    Set<Long> updated = new LinkedHashSet<>(memberIds);
    updated.addAll(candidateIds);
    return new Workspace(id, academyId, name, createdBy, updated);
  }

  // 요청된 후보 중 아직 참여 중이 아닌 사용자만 반환 (참여자 추가 응답·멱등 처리에 사용)
  public Set<Long> newlyAddedMemberIds(Set<Long> candidateIds) {
    Set<Long> newlyAdded = new LinkedHashSet<>(candidateIds);
    newlyAdded.removeAll(memberIds);
    return newlyAdded;
  }

  public Workspace removeMember(Long targetUserId) {
    if (!memberIds.contains(targetUserId)) {
      throw new WorkspaceMemberNotFoundException();
    }
    if (memberIds.size() <= 1) {
      throw new WorkspaceLastMemberException();
    }
    Set<Long> updated = new LinkedHashSet<>(memberIds);
    updated.remove(targetUserId);
    return new Workspace(id, academyId, name, createdBy, updated);
  }

  public Workspace recover(String finalName) {
    return new Workspace(id, academyId, finalName, createdBy, memberIds);
  }
}
