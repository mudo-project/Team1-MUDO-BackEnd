package com.academy.mudogroupware.workspace.presentation.api;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.usecase.CreateWorkspaceUseCase;
import com.academy.mudogroupware.workspace.presentation.api.common.WorkspaceResponseCode;
import com.academy.mudogroupware.workspace.presentation.api.request.CreateWorkspaceRequest;
import com.academy.mudogroupware.workspace.presentation.api.response.CreateWorkspaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 @PreAuthorize를 추가한다.
@Tag(name = "워크스페이스", description = "워크스페이스 생성 및 관리 API")
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

  private final CreateWorkspaceUseCase createWorkspaceUseCase;

  @Operation(summary = "워크스페이스 생성", description = "생성자는 자동 참여하며, 추가 참여자는 같은 학원의 활성 사용자만 등록할 수 있습니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "워크스페이스 생성 성공"),
    @ApiResponse(responseCode = "400", description = "요청값 또는 참여자가 유효하지 않음"),
    @ApiResponse(responseCode = "409", description = "이름 충돌 재시도 후 생성 실패")
  })
  @PostMapping
  public ResponseEntity<GlobalApiResponse<CreateWorkspaceResponse>> createWorkspace(
      @AuthenticationPrincipal AuthUser authUser,
      @Valid @RequestBody CreateWorkspaceRequest request
  ) {
    Long workspaceId = createWorkspaceUseCase.createWorkspace(request.toCommand(authUser));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            GlobalApiResponse.created(
                WorkspaceResponseCode.WORKSPACE_CREATED,
                CreateWorkspaceResponse.from(workspaceId)));
  }
}
