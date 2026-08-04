package com.academy.mudogroupware.workspace.domain.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Workspace {

  private final Long id;
  private final Long academyId;
  private final String name;
  private final Long createdBy;
  private final Set<Long> memberIds;

  @Builder
  private Workspace(Long id, Long academyId, String name, Long createdBy, Set<Long> memberIds) {
    this.id = id;
    this.academyId = academyId;
    this.name = name;
    this.createdBy = createdBy;
    Set<Long> members = new LinkedHashSet<>(memberIds);
    members.add(createdBy);
    this.memberIds = Collections.unmodifiableSet(members);
  }
}
