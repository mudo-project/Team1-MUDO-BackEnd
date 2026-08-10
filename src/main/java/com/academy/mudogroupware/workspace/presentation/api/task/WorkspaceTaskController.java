package com.academy.mudogroupware.workspace.presentation.api.task;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.task.DeleteTaskCommand;
import com.academy.mudogroupware.workspace.application.query.task.TaskDetail;
import com.academy.mudogroupware.workspace.application.usecase.task.CreateTaskUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.DeleteTaskUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.TaskDetailQueryUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.UpdateTaskUseCase;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.presentation.api.common.WorkspaceResponseCode;
import com.academy.mudogroupware.workspace.presentation.api.request.task.CreateTaskRequest;
import com.academy.mudogroupware.workspace.presentation.api.request.task.UpdateTaskRequest;
import com.academy.mudogroupware.workspace.presentation.api.response.task.CreateTaskResponse;
import com.academy.mudogroupware.workspace.presentation.api.response.task.TaskDetailResponse;
import com.academy.mudogroupware.workspace.presentation.api.response.task.TaskUpdateResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 @PreAuthorize를 추가한다.
@Tag(name = "업무", description = "워크스페이스 업무 생성 및 관리 API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tasks")
@RequiredArgsConstructor
public class WorkspaceTaskController {

  private final CreateTaskUseCase createTaskUseCase;
  private final TaskDetailQueryUseCase taskDetailQueryUseCase;
  private final UpdateTaskUseCase updateTaskUseCase;
  private final DeleteTaskUseCase deleteTaskUseCase;

  @Operation(
      summary = "업무 생성",
      description = "현재 참여자만 생성할 수 있습니다. 마감일이 과거이면 지연 상태로 생성됩니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "업무 생성 성공"),
    @ApiResponse(responseCode = "400", description = "제목 또는 마감일이 유효하지 않음"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스가 존재하지 않거나 삭제됨")
  })
  @PostMapping
  public ResponseEntity<GlobalApiResponse<CreateTaskResponse>> createTask(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @Valid @RequestBody CreateTaskRequest request) {
    Long taskId = createTaskUseCase.createTask(request.toCommand(authUser, workspaceId));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            GlobalApiResponse.created(
                WorkspaceResponseCode.TASK_CREATED, CreateTaskResponse.from(taskId)));
  }

  @Operation(
      summary = "업무 상세 조회",
      description = "현재 참여자만 조회할 수 있습니다. 최종 상태 변경 이력이 없으면 lastStatusChangedAt 필드가 생략됩니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "업무 상세 조회 성공"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스 또는 업무가 존재하지 않음")
  })
  @GetMapping("/{taskId}")
  public ResponseEntity<GlobalApiResponse<TaskDetailResponse>> getTaskDetail(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId) {
    TaskDetail detail = taskDetailQueryUseCase.getTaskDetail(workspaceId, taskId, authUser.userId());
    return ResponseEntity.ok(
        GlobalApiResponse.ok(WorkspaceResponseCode.TASK_DETAIL_RETRIEVED, TaskDetailResponse.from(detail)));
  }

  @Operation(
      summary = "업무 상태·마감일 수정",
      description =
          "현재 참여자만 수정할 수 있습니다. 상태와 마감일 중 최소 하나를 보내야 합니다. "
              + "기한이 지난 업무를 대기·진행 중으로 되돌릴 때는 오늘 이후의 새 마감일을 함께 보내야 합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "업무 수정 성공"),
    @ApiResponse(responseCode = "400", description = "요청값이 유효하지 않거나 허용되지 않은 상태 전이"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스 또는 업무가 존재하지 않음")
  })
  @PatchMapping("/{taskId}")
  public ResponseEntity<GlobalApiResponse<TaskUpdateResponse>> updateTask(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId,
      @Valid @RequestBody UpdateTaskRequest request) {
    Task updated = updateTaskUseCase.updateTask(request.toCommand(authUser, workspaceId, taskId));
    return ResponseEntity.ok(
        GlobalApiResponse.ok(
            WorkspaceResponseCode.TASK_UPDATED, TaskUpdateResponse.from(updated)));
  }

  @Operation(
      summary = "업무 삭제",
      description =
          "현재 참여자만 삭제할 수 있습니다. 하드 삭제이며 댓글·멘션·상태 이력이 함께 삭제되고 복구할 수 없습니다. "
              + "반복 업무의 회차를 삭제하면 해당 회차를 영구히 건너뜁니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "업무 삭제 성공"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스 또는 업무가 존재하지 않음")
  })
  @DeleteMapping("/{taskId}")
  public ResponseEntity<GlobalApiResponse<Void>> deleteTask(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long taskId) {
    deleteTaskUseCase.deleteTask(new DeleteTaskCommand(workspaceId, taskId, authUser.userId()));
    return ResponseEntity.ok(GlobalApiResponse.ok(WorkspaceResponseCode.TASK_DELETED));
  }
}
