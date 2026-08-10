package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.global.infrastructure.logging.AfterCommitLogger;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
    log.info(
        "event=workspace_recover_시작 workspaceId={}, requesterId={}",
        command.workspaceId(),
        command.requesterId());

    Workspace workspace =
        workspaceRepository
            .findDeletedByIdForUpdate(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

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

    AfterCommitLogger.run(
        () ->
            log.info(
                "event=workspace_recover_완료 workspaceId={}, name={}",
                command.workspaceId(),
                recovered.getName()));
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
