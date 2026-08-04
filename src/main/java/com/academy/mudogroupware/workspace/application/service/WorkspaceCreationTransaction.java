package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class WorkspaceCreationTransaction {

  private static final int MAX_NAME_LENGTH = 100;

  private final WorkspaceRepository workspaceRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Long create(Long academyId, Long creatorId, String baseName, Set<Long> memberIds) {
    String availableName = findAvailableName(academyId, baseName);
    Workspace workspace =
        Workspace.builder()
            .academyId(academyId)
            .name(availableName)
            .createdBy(creatorId)
            .memberIds(memberIds)
            .build();

    return workspaceRepository.save(workspace).getId();
  }

  private String findAvailableName(Long academyId, String baseName) {
    if (!workspaceRepository.existsByAcademyIdAndName(academyId, baseName)) {
      return baseName;
    }

    int suffixNumber = 1;
    while (true) {
      String candidate = withSuffix(baseName, suffixNumber++);
      if (!workspaceRepository.existsByAcademyIdAndName(academyId, candidate)) {
        return candidate;
      }
    }
  }

  private String withSuffix(String baseName, int suffixNumber) {
    String suffix = " (" + suffixNumber + ")";
    int availableBaseLength = MAX_NAME_LENGTH - suffix.length();
    String shortenedBase =
        baseName.length() > availableBaseLength
            ? baseName.substring(0, availableBaseLength)
            : baseName;
    return shortenedBase + suffix;
  }
}
