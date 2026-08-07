package com.academy.mudogroupware.workspace.presentation.api;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.DeleteTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.command.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.application.usecase.CreateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.application.usecase.DeleteTaskCommentUseCase;
import com.academy.mudogroupware.workspace.application.usecase.ToggleTaskCommentCompleteUseCase;
import com.academy.mudogroupware.workspace.application.usecase.UpdateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;
import com.academy.mudogroupware.workspace.presentation.api.common.WorkspaceResponseCode;
import com.academy.mudogroupware.workspace.presentation.api.request.CreateTaskCommentRequest;
import com.academy.mudogroupware.workspace.presentation.api.request.UpdateTaskCommentRequest;
import com.academy.mudogroupware.workspace.presentation.api.response.TaskCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 @PreAuthorize를 추가한다.
@Tag(name = "업무 댓글", description = "업무 댓글 및 멘션 CRUD API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class WorkspaceTaskCommentController {

  private final CreateTaskCommentUseCase createTaskCommentUseCase;
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
    @ApiResponse(responseCode = "204", description = "댓글 삭제 성공"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스·업무·댓글이 존재하지 않음")
  })
  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> deleteComment(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId,
      @PathVariable Long commentId) {
    deleteTaskCommentUseCase.deleteComment(
        new DeleteTaskCommentCommand(workspaceId, taskId, commentId, authUser.userId()));
    return ResponseEntity.noContent().build();
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
