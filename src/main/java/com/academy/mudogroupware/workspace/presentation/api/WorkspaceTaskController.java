package com.academy.mudogroupware.workspace.presentation.api;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.usecase.CreateTaskUseCase;
import com.academy.mudogroupware.workspace.presentation.api.common.WorkspaceResponseCode;
import com.academy.mudogroupware.workspace.presentation.api.request.CreateTaskRequest;
import com.academy.mudogroupware.workspace.presentation.api.response.CreateTaskResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
