package com.academy.mudogroupware.workspace.domain.model;

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
    this.memberIds = Set.copyOf(memberIds);
  }
}
