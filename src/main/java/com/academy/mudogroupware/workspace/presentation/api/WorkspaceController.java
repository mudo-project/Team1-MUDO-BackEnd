package com.academy.mudogroupware.workspace.presentation.api;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.query.WorkspaceListScope;
import com.academy.mudogroupware.workspace.application.usecase.CreateWorkspaceUseCase;
import com.academy.mudogroupware.workspace.application.usecase.RecordWorkspaceRecentAccessUseCase;
import com.academy.mudogroupware.workspace.application.usecase.WorkspaceQueryUseCase;
import com.academy.mudogroupware.workspace.presentation.api.common.WorkspaceResponseCode;
import com.academy.mudogroupware.workspace.presentation.api.request.CreateWorkspaceRequest;
import com.academy.mudogroupware.workspace.presentation.api.response.CreateWorkspaceResponse;
import com.academy.mudogroupware.workspace.presentation.api.response.WorkspaceListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 @PreAuthorize를 추가한다.
@Tag(name = "워크스페이스", description = "워크스페이스 생성 및 관리 API")
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

  private static final String WORKSPACE_READ_ALL_AUTHORITY = "WORKSPACE:READ_ALL";

  private final CreateWorkspaceUseCase createWorkspaceUseCase;
  private final WorkspaceQueryUseCase workspaceQueryUseCase;
  private final RecordWorkspaceRecentAccessUseCase recordWorkspaceRecentAccessUseCase;

  @Operation(
      summary = "워크스페이스 목록 조회",
      description = "내 워크스페이스를 조회하며, 전체 조회에는 WORKSPACE:READ_ALL 권한이 필요합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "워크스페이스 목록 조회 성공"),
    @ApiResponse(responseCode = "400", description = "scope 값이 유효하지 않음"),
    @ApiResponse(responseCode = "403", description = "전체 조회 권한이 없음")
  })
  @PreAuthorize(
      "#p1 != T(com.academy.mudogroupware.workspace.application.query.WorkspaceListScope).ALL"
          + " or hasAuthority('WORKSPACE:READ_ALL')")
  @GetMapping
  public ResponseEntity<GlobalApiResponse<List<WorkspaceListResponse>>> getWorkspaces(
      @AuthenticationPrincipal AuthUser authUser,
      @RequestParam(defaultValue = "MINE") WorkspaceListScope scope) {
    List<WorkspaceListResponse> response =
        workspaceQueryUseCase.getWorkspaces(authUser.academyId(), authUser.userId(), scope).stream()
            .map(WorkspaceListResponse::from)
            .toList();

    return ResponseEntity.ok(
        GlobalApiResponse.ok(WorkspaceResponseCode.WORKSPACE_LIST_RETRIEVED, response));
  }

  @Operation(summary = "워크스페이스 생성", description = "생성자는 자동 참여하며, 추가 참여자는 같은 학원의 활성 사용자만 등록할 수 있습니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "워크스페이스 생성 성공"),
    @ApiResponse(responseCode = "400", description = "요청값 또는 참여자가 유효하지 않음"),
    @ApiResponse(responseCode = "409", description = "같은 학원에 동일한 활성 워크스페이스 이름이 존재함")
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

  @Operation(
      summary = "워크스페이스 최근 접속 기록",
      description = "접근 가능한 워크스페이스의 최근 접속 시각을 기록합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "최근 접속 기록 성공"),
    @ApiResponse(responseCode = "403", description = "워크스페이스 접근 권한이 없음")
  })
  @PutMapping("/{workspaceId}/recent-access")
  public ResponseEntity<Void> recordRecentAccess(
      @AuthenticationPrincipal AuthUser authUser,
      Authentication authentication,
      @PathVariable Long workspaceId) {
    boolean canReadAll =
        authentication.getAuthorities().stream()
            .anyMatch(authority -> WORKSPACE_READ_ALL_AUTHORITY.equals(authority.getAuthority()));

    recordWorkspaceRecentAccessUseCase.recordRecentAccess(
        authUser.academyId(), authUser.userId(), workspaceId, canReadAll);
    return ResponseEntity.noContent().build();
  }
}
