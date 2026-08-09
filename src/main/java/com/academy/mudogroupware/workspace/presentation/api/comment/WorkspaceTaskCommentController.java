package com.academy.mudogroupware.workspace.presentation.api.comment;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.comment.DeleteTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.command.comment.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentListItem;
import com.academy.mudogroupware.workspace.application.usecase.comment.CreateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.DeleteTaskCommentUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.TaskCommentListQueryUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.ToggleTaskCommentCompleteUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.UpdateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.presentation.api.common.WorkspaceResponseCode;
import com.academy.mudogroupware.workspace.presentation.api.request.comment.CreateTaskCommentRequest;
import com.academy.mudogroupware.workspace.presentation.api.request.comment.UpdateTaskCommentRequest;
import com.academy.mudogroupware.workspace.presentation.api.response.comment.TaskCommentListItemResponse;
import com.academy.mudogroupware.workspace.presentation.api.response.comment.TaskCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 @PreAuthorize를 추가한다.
@Tag(name = "업무 댓글", description = "업무 댓글 및 멘션 CRUD API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tasks/{taskId}/comments")
@RequiredArgsConstructor
@Validated
public class WorkspaceTaskCommentController {

  private final CreateTaskCommentUseCase createTaskCommentUseCase;
  private final TaskCommentListQueryUseCase taskCommentListQueryUseCase;
  private final UpdateTaskCommentUseCase updateTaskCommentUseCase;
  private final DeleteTaskCommentUseCase deleteTaskCommentUseCase;
  private final ToggleTaskCommentCompleteUseCase toggleTaskCommentCompleteUseCase;

  @Operation(
      summary = "업무 댓글 생성",
      description = "현재 참여자만 생성할 수 있습니다. 멘션 대상은 워크스페이스 참여자여야 합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "댓글 생성 성공"),
    @ApiResponse(responseCode = "400", description = "내용이 비어있거나 멘션 대상이 참여자가 아님"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스 또는 업무가 존재하지 않음")
  })
  @PostMapping
  public ResponseEntity<GlobalApiResponse<TaskCommentResponse>> createComment(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId,
      @Valid @RequestBody CreateTaskCommentRequest request) {
    TaskComment comment =
        createTaskCommentUseCase.createComment(request.toCommand(authUser, workspaceId, taskId));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            GlobalApiResponse.created(
                WorkspaceResponseCode.TASK_COMMENT_CREATED, TaskCommentResponse.from(comment)));
  }

  @Operation(
      summary = "댓글 목록 조회",
      description = "현재 참여자만 조회할 수 있습니다. 생성일 오름차순(오래된 댓글 먼저) 페이지네이션 응답입니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "댓글 목록 조회 성공"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스 또는 업무가 존재하지 않음")
  })
  @GetMapping
  public ResponseEntity<GlobalApiResponse<SliceResponse<TaskCommentListItemResponse>>> getComments(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    PageResult<TaskCommentListItem> comments =
        taskCommentListQueryUseCase.getComments(workspaceId, taskId, authUser.userId(), page, size);
    SliceResponse<TaskCommentListItemResponse> response =
        SliceResponse.from(comments, TaskCommentListItemResponse::from);
    return ResponseEntity.ok(
        GlobalApiResponse.ok(WorkspaceResponseCode.TASK_COMMENT_LIST_RETRIEVED, response));
  }

  @Operation(
      summary = "업무 댓글 수정",
      description = "현재 참여자 누구나 수정할 수 있습니다. 멘션 목록은 요청한 내용으로 전체 교체됩니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "댓글 수정 성공"),
    @ApiResponse(responseCode = "400", description = "내용이 비어있거나 멘션 대상이 참여자가 아님"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스·업무·댓글이 존재하지 않음")
  })
  @PatchMapping("/{commentId}")
  public ResponseEntity<GlobalApiResponse<TaskCommentResponse>> updateComment(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId,
      @PathVariable Long commentId,
      @Valid @RequestBody UpdateTaskCommentRequest request) {
    TaskComment comment =
        updateTaskCommentUseCase.updateComment(
            request.toCommand(authUser, workspaceId, taskId, commentId));
    return ResponseEntity.ok(
        GlobalApiResponse.ok(
            WorkspaceResponseCode.TASK_COMMENT_UPDATED, TaskCommentResponse.from(comment)));
  }

  @Operation(
      summary = "업무 댓글 삭제",
      description = "현재 참여자 누구나 삭제할 수 있습니다. 하드 삭제이며 멘션도 함께 삭제되고 복구할 수 없습니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스·업무·댓글이 존재하지 않음")
  })
  @DeleteMapping("/{commentId}")
  public ResponseEntity<GlobalApiResponse<Void>> deleteComment(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId,
      @PathVariable Long commentId) {
    deleteTaskCommentUseCase.deleteComment(
        new DeleteTaskCommentCommand(workspaceId, taskId, commentId, authUser.userId()));
    return ResponseEntity.ok(GlobalApiResponse.ok(WorkspaceResponseCode.TASK_COMMENT_DELETED));
  }

  @Operation(
      summary = "업무 댓글 완료 토글",
      description = "현재 참여자 누구나 완료↔취소를 전환할 수 있습니다. 마지막으로 전환한 참여자가 완료 처리자로 기록됩니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "완료 상태 변경 성공"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스·업무·댓글이 존재하지 않음")
  })
  @PatchMapping("/{commentId}/complete")
  public ResponseEntity<GlobalApiResponse<TaskCommentResponse>> toggleComplete(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId,
      @PathVariable Long commentId) {
    TaskComment comment =
        toggleTaskCommentCompleteUseCase.toggleComplete(
            new ToggleTaskCommentCompleteCommand(workspaceId, taskId, commentId, authUser.userId()));
    return ResponseEntity.ok(
        GlobalApiResponse.ok(
            WorkspaceResponseCode.TASK_COMMENT_COMPLETE_TOGGLED, TaskCommentResponse.from(comment)));
  }
}
