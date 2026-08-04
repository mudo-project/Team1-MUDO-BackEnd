package com.academy.mudogroupware.workspace.domain.repository;

import com.academy.mudogroupware.workspace.domain.model.Workspace;

public interface WorkspaceRepository {

  Workspace save(Workspace workspace);

  boolean existsByAcademyIdAndName(Long academyId, String name);
}
