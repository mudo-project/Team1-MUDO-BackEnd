package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.port.WorkspaceDetailQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceUserInfoPort;
import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentSummary;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceDetail;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.application.query.task.WorkspaceTaskCandidate;
import com.academy.mudogroupware.workspace.application.query.task.WorkspaceTaskItem;
import com.academy.mudogroupware.workspace.application.usecase.workspace.WorkspaceDetailQueryUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceDetailQueryService implements WorkspaceDetailQueryUseCase {

  private static final String UNKNOWN_NAME = "알 수 없음";

  // 업무 정렬
  private static final Comparator<WorkspaceTaskCandidate> TASK_ORDER =
      Comparator.comparing(WorkspaceTaskCandidate::status) // 상태
          // 마감일 null 이면 뒤로
          .thenComparing(
                  WorkspaceTaskCandidate::dueAt,
                  Comparator.nullsLast(Comparator.naturalOrder())
          )

          .thenComparing(WorkspaceTaskCandidate::createdAt)
          .thenComparing(WorkspaceTaskCandidate::taskId);

  private final WorkspaceDetailQueryPort workspaceDetailQueryPort;
  private final WorkspaceListQueryPort workspaceListQueryPort;
  private final WorkspaceUserInfoPort workspaceUserInfoPort;

  @Override
  public WorkspaceDetail getWorkspaceDetail(
      Long userId, Long workspaceId, LocalDate date, boolean canReadAll
  ) {
    log.info(
        "event=workspace_detail_시작 workspaceId={}, date={}",
        workspaceId,
        date);

    // 워크스페이스 존재 여부 확인
    String name = workspaceDetailQueryPort.findActiveWorkspaceName(workspaceId)
            .orElseThrow(WorkspaceNotFoundException::new);

    // 워크스페이스 접근 권한 확인
    if (!workspaceListQueryPort.existsAccessible(workspaceId, userId, canReadAll)) {
      throw new WorkspaceAccessDeniedException();
    }

    // 워크스페이스 참여자 업무 조회
    List<Long> memberIds = workspaceDetailQueryPort.findMemberIds(workspaceId);

    // 해당하는 날 업무 조회
    List<WorkspaceTaskCandidate> candidates =
        workspaceDetailQueryPort.findVisibleTasks(workspaceId, date);

    // 사용자 정보 일괄 조회 (참여자, 업무 생성자)
    Set<Long> userIdsToResolve = new LinkedHashSet<>(memberIds);
    candidates.forEach(candidate -> userIdsToResolve.add(candidate.createdBy()));
    // userId 모아서 한번에 조회
    Map<Long, String> nameByUserId =
        workspaceUserInfoPort.findUserInfo(userIdsToResolve).stream()
            .collect(Collectors.toMap(WorkspaceMemberInfo::userId, WorkspaceMemberInfo::name));

    // 참여자 번호, 이름 조회
    List<WorkspaceMemberInfo> members =
        memberIds.stream().map(id ->
                new WorkspaceMemberInfo(
                        id, resolveName(id, nameByUserId)
                )
        ).toList();

    // 댓글 수 일괄 조회
    Map<Long, TaskCommentSummary> commentSummaryByTaskId =
        workspaceDetailQueryPort
            .findCommentSummaries(    // 업무 별 완료 댓글, 전체 댓글 조회
                    candidates.stream()
                            .map(WorkspaceTaskCandidate::taskId)
                            .toList()
            )
            .stream()
            .collect(Collectors.toMap(
                    TaskCommentSummary::taskId,
                    summary -> summary)
            );

    List<WorkspaceTaskItem> tasks =
        candidates.stream()
            .sorted(TASK_ORDER)
            .map(candidate
                    -> toTaskItem(
                            candidate,
                            nameByUserId,
                            commentSummaryByTaskId
                    )
            )
            .toList();

    log.info(
        "event=workspace_detail_완료 workspaceId={}, taskCount={}",
        workspaceId,
        tasks.size());
    // 최종 워크스페이스 상세 조회 리턴
    return new WorkspaceDetail(
            workspaceId, name, members, tasks);
  }


  // 업무 응답 조합, 응답용 item으로 변환
  private WorkspaceTaskItem toTaskItem(
      WorkspaceTaskCandidate candidate,
      Map<Long, String> nameByUserId,
      Map<Long, TaskCommentSummary> commentSummaryByTaskId) {
    // 작성자도 id, name 으로
    WorkspaceMemberInfo creator =
        new WorkspaceMemberInfo(
            candidate.createdBy(),
                resolveName(        // 생성자 id로 Map을 조회 이름 추출
                        candidate.createdBy(),  // 생성자 id
                        nameByUserId // userId별 사용자 이름 조회표
                )
        );
    // 댓글 요약
    TaskCommentSummary summary = commentSummaryByTaskId.get(candidate.taskId());

    return new WorkspaceTaskItem(
        candidate.taskId(),
        candidate.title(),
        candidate.status(),
        creator,
        candidate.dueAt(),
        summary == null ? null : summary.completedCount(),
        summary == null ? null : summary.totalCount());
  }

  // 이름 조회 실패시 알 수 없음 반환
  private String resolveName(Long userId, Map<Long, String> nameByUserId) {
    return nameByUserId.getOrDefault(userId, UNKNOWN_NAME);
  }
}
