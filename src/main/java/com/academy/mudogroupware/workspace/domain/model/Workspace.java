package com.academy.mudogroupware.workspace.domain.model;

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
}
