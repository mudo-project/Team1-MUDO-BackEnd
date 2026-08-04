package com.academy.mudogroupware.approval.presentation.api;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.approval.application.query.ApprovalTemplateDetailView;
import com.academy.mudogroupware.approval.application.usecase.ApprovalTemplateQueryUseCase;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.application.usecase.DeleteApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.application.usecase.UpdateApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.presentation.api.common.ApprovalResponseCode;
import com.academy.mudogroupware.approval.presentation.api.request.CreateApprovalTemplateRequest;
import com.academy.mudogroupware.approval.presentation.api.request.UpdateApprovalTemplateRequest;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalTemplateCreateResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalTemplateDetailResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalTemplateSummaryResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO: 템플릿 생성/수정/삭제는 행정직원만 가능해야 함 - users.role 값 체계 확정되면 Application/Domain Policy에 권한 판단 반영
@Tag(name = "결재 템플릿", description = "결재 템플릿 생성/조회/수정/삭제 API")
@RestController
@RequestMapping("/api/approval-templates")
@RequiredArgsConstructor
public class ApprovalTemplateController {

    private final CreateApprovalTemplateUseCase createApprovalTemplateUseCase;
    private final UpdateApprovalTemplateUseCase updateApprovalTemplateUseCase;
    private final DeleteApprovalTemplateUseCase deleteApprovalTemplateUseCase;
    private final ApprovalTemplateQueryUseCase approvalTemplateQueryUseCase;

    @Operation(summary = "결재 템플릿 생성", description = "이름과 기본 결재선(순서 있는 담당자 목록)을 지정해 템플릿을 만든다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<ApprovalTemplateCreateResponse>> createTemplate(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateApprovalTemplateRequest request) {
        Long templateId = createApprovalTemplateUseCase.createTemplate(request.toCommand(authUser.userId()));
        ApprovalTemplateCreateResponse data = ApprovalTemplateCreateResponse.from(templateId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(ApprovalResponseCode.TEMPLATE_CREATED, data));
    }

    @Operation(summary = "결재 템플릿 목록 조회", description = "요청자 소속 학원의 템플릿만 페이지 단위로 조회한다. 결재선 담당자 이름을 함께 반환한다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<SliceResponse<ApprovalTemplateSummaryResponse>>> getTemplates(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SliceResponse<ApprovalTemplateSummaryResponse> data = SliceResponse.from(
                approvalTemplateQueryUseCase.getTemplates(authUser.userId(), page, size),
                ApprovalTemplateSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.TEMPLATE_LIST_RETRIEVED, data));
    }

    @Operation(summary = "결재 템플릿 상세 조회")
    @GetMapping("/{templateId}")
    public ResponseEntity<GlobalApiResponse<ApprovalTemplateDetailResponse>> getTemplateDetail(@PathVariable Long templateId) {
        ApprovalTemplateDetailView view = approvalTemplateQueryUseCase.getTemplateDetail(templateId);
        ApprovalTemplateDetailResponse data = ApprovalTemplateDetailResponse.from(view);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.TEMPLATE_DETAIL_RETRIEVED, data));
    }

    @Operation(summary = "결재 템플릿 수정", description = "이름·결재선을 요청 값으로 전체 교체한다.")
    @PatchMapping("/{templateId}")
    public ResponseEntity<Void> updateTemplate(@PathVariable Long templateId,
                                                @Valid @RequestBody UpdateApprovalTemplateRequest request) {
        updateApprovalTemplateUseCase.updateTemplate(request.toCommand(templateId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "결재 템플릿 삭제")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        deleteApprovalTemplateUseCase.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}
