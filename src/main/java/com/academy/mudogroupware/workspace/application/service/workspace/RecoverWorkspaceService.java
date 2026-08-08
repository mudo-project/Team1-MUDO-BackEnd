package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.command.workspace.RecoverWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.usecase.workspace.RecoverWorkspaceUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecoverWorkspaceService implements RecoverWorkspaceUseCase {

  private static final DateTimeFormatter SUFFIX_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  private static final int MAX_NAME_LENGTH = 100;

  private final WorkspaceRepository workspaceRepository;
  private final Clock clock;

  @Override
  @Transactional
  public String recover(RecoverWorkspaceCommand command) {
    Workspace workspace =
        workspaceRepository
            .findDeletedByIdForUpdate(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    String originalName = workspace.getName();
    String finalName =
        workspaceRepository.existsByAcademyIdAndName(workspace.getAcademyId(), originalName)
            ? suffixedName(originalName)
            : originalName;

    Workspace recovered = workspace.recover(finalName);
    workspaceRepository.recover(command.workspaceId(), recovered.getName());
    return recovered.getName();
  }

  private String suffixedName(String originalName) {
    String suffix = "(" + SUFFIX_FORMATTER.format(LocalDateTime.now(clock)) + ")";
    int maxOriginalLength = MAX_NAME_LENGTH - suffix.length();
    String truncatedOriginal =
        originalName.length() > maxOriginalLength
            ? originalName.substring(0, maxOriginalLength)
            : originalName;
    return truncatedOriginal + suffix;
  }
}
