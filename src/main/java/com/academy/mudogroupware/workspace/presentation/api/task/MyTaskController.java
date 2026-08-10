package com.academy.mudogroupware.workspace.presentation.api.task;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.application.usecase.task.MyTaskListQueryUseCase;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.presentation.api.common.WorkspaceResponseCode;
import com.academy.mudogroupware.workspace.presentation.api.response.task.MyTaskListItemResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "내 업무", description = "내가 속한 모든 워크스페이스의 업무를 모아보는 API")
@RestController
@RequestMapping("/api/tasks/me")
@RequiredArgsConstructor
@Validated
public class MyTaskController {

  private final MyTaskListQueryUseCase myTaskListQueryUseCase;

  @Operation(
      summary = "내 업무 모아보기",
      description =
          "내가 참여자로 속한 모든 워크스페이스의 업무 중 대기·진행중·지연 상태만 모아 기한 오름차순으로 반환합니다. "
              + "완료된 업무는 필터로도 조회할 수 없습니다.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "내 업무 목록 조회 성공")})
  @GetMapping
  public ResponseEntity<GlobalApiResponse<SliceResponse<MyTaskListItemResponse>>> getMyTasks(
      @AuthenticationPrincipal AuthUser authUser,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) Long workspaceId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    PageResult<MyTaskListItem> tasks =
        myTaskListQueryUseCase.getMyTasks(authUser.userId(), status, workspaceId, page, size);
    SliceResponse<MyTaskListItemResponse> response =
        SliceResponse.from(tasks, MyTaskListItemResponse::from);
    return ResponseEntity.ok(
        GlobalApiResponse.ok(WorkspaceResponseCode.MY_TASK_LIST_RETRIEVED, response));
  }
}
