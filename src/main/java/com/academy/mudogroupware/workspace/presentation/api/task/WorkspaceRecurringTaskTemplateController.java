package com.academy.mudogroupware.workspace.presentation.api.task;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.workspace.application.command.task.DeleteRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.CreateRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.DeleteRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.GetRecurringTaskTemplatesUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.UpdateRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import com.academy.mudogroupware.workspace.presentation.api.common.WorkspaceResponseCode;
import com.academy.mudogroupware.workspace.presentation.api.request.task.CreateRecurringTaskTemplateRequest;
import com.academy.mudogroupware.workspace.presentation.api.request.task.UpdateRecurringTaskTemplateRequest;
import com.academy.mudogroupware.workspace.presentation.api.response.task.CreateRecurringTaskTemplateResponse;
import com.academy.mudogroupware.workspace.presentation.api.response.task.RecurringTaskTemplateListResponse;
import com.academy.mudogroupware.workspace.presentation.api.response.task.UpdateRecurringTaskTemplateResponse;
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
import org.springframework.security.core.Authentication;
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

@Tag(name = "반복 업무 템플릿", description = "워크스페이스 반복 업무 템플릿 생성 및 관리 API")
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/recurring-templates")
@RequiredArgsConstructor
@Validated
public class WorkspaceRecurringTaskTemplateController {

  private final CreateRecurringTaskTemplateUseCase createRecurringTaskTemplateUseCase;
  private final GetRecurringTaskTemplatesUseCase getRecurringTaskTemplatesUseCase;
  private final UpdateRecurringTaskTemplateUseCase updateRecurringTaskTemplateUseCase;
  private final DeleteRecurringTaskTemplateUseCase deleteRecurringTaskTemplateUseCase;

  @Operation(
      summary = "반복 업무 템플릿 목록 조회",
      description = "현재 참여자만 조회할 수 있습니다. 최신 생성순으로 페이지 단위(기본 20개)로 반환합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
    @ApiResponse(responseCode = "400", description = "page 또는 size 값이 유효하지 않음"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스가 존재하지 않거나 삭제됨")
  })
  @GetMapping
  public ResponseEntity<GlobalApiResponse<SliceResponse<RecurringTaskTemplateListResponse>>> getTemplates(
      @AuthenticationPrincipal AuthUser authUser,
      Authentication authentication,
      @PathVariable Long workspaceId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    boolean canReadAll =
        authentication.getAuthorities().stream()
            .anyMatch(authority -> "WORKSPACE:READ_ALL".equals(authority.getAuthority()));
    SliceResponse<RecurringTaskTemplateListResponse> response =
        SliceResponse.from(
            getRecurringTaskTemplatesUseCase.getTemplates(
                workspaceId, authUser.userId(), page, size, canReadAll),
            RecurringTaskTemplateListResponse::from);
    return ResponseEntity.ok(
        GlobalApiResponse.ok(WorkspaceResponseCode.RECURRING_TEMPLATE_LIST_RETRIEVED, response));
  }

  @Operation(
      summary = "반복 업무 템플릿 생성",
      description = "현재 참여자만 생성할 수 있습니다. recurrenceRule은 주기 타입에 맞는 부가 정보를 담습니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "템플릿 생성 성공"),
    @ApiResponse(responseCode = "400", description = "제목 또는 반복 주기 설정이 유효하지 않음"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스가 존재하지 않거나 삭제됨")
  })
  @PostMapping
  public ResponseEntity<GlobalApiResponse<CreateRecurringTaskTemplateResponse>> createTemplate(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @Valid @RequestBody CreateRecurringTaskTemplateRequest request) {
    Long templateId =
        createRecurringTaskTemplateUseCase.create(request.toCommand(authUser, workspaceId));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            GlobalApiResponse.created(
                WorkspaceResponseCode.RECURRING_TEMPLATE_CREATED,
                CreateRecurringTaskTemplateResponse.from(templateId)));
  }

  @Operation(
      summary = "반복 업무 템플릿 수정",
      description = "현재 참여자만 수정할 수 있습니다. 제목 단독 또는 반복 주기(recurrenceType+recurrenceRule) 세트로 수정합니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "템플릿 수정 성공"),
    @ApiResponse(responseCode = "400", description = "제목·반복 주기 설정이 유효하지 않거나 아무 필드도 없음"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스 또는 템플릿이 존재하지 않음")
  })
  @PatchMapping("/{templateId}")
  public ResponseEntity<GlobalApiResponse<UpdateRecurringTaskTemplateResponse>> updateTemplate(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long templateId,
      @Valid @RequestBody UpdateRecurringTaskTemplateRequest request) {
    RecurringTaskTemplate updated =
        updateRecurringTaskTemplateUseCase.update(
            request.toCommand(authUser, workspaceId, templateId));
    return ResponseEntity.ok(
        GlobalApiResponse.ok(
            WorkspaceResponseCode.RECURRING_TEMPLATE_UPDATED,
            UpdateRecurringTaskTemplateResponse.from(updated)));
  }

  @Operation(
      summary = "반복 업무 템플릿 삭제",
      description =
          "현재 참여자만 삭제할 수 있습니다. 하드 삭제이며 복구할 수 없습니다. "
              + "이미 생성된 업무는 삭제되지 않고 recurring_template_id만 비워진 일반 업무로 남습니다.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "템플릿 삭제 성공"),
    @ApiResponse(responseCode = "403", description = "참여자가 아님"),
    @ApiResponse(responseCode = "404", description = "워크스페이스 또는 템플릿이 존재하지 않음")
  })
  @DeleteMapping("/{templateId}")
  public ResponseEntity<GlobalApiResponse<Void>> deleteTemplate(
      @AuthenticationPrincipal AuthUser authUser,
      @PathVariable Long workspaceId,
      @PathVariable Long templateId) {
    deleteRecurringTaskTemplateUseCase.delete(
        new DeleteRecurringTaskTemplateCommand(workspaceId, templateId, authUser.userId()));
    return ResponseEntity.ok(GlobalApiResponse.ok(WorkspaceResponseCode.RECURRING_TEMPLATE_DELETED));
  }
}
