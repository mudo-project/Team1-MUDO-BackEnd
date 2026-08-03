package com.academy.mudogroupware.approval.presentation.api;

import java.util.List;

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
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO: 템플릿 생성/수정/삭제는 행정직원만 가능해야 함 - users.role 값 체계 확정되면 Application/Domain Policy에 권한 판단 반영
@RestController
@RequestMapping("/api/approval-templates")
@RequiredArgsConstructor
public class ApprovalTemplateController {

    private final CreateApprovalTemplateUseCase createApprovalTemplateUseCase;
    private final UpdateApprovalTemplateUseCase updateApprovalTemplateUseCase;
    private final DeleteApprovalTemplateUseCase deleteApprovalTemplateUseCase;
    private final ApprovalTemplateQueryUseCase approvalTemplateQueryUseCase;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<ApprovalTemplateCreateResponse>> createTemplate(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateApprovalTemplateRequest request) {
        Long templateId = createApprovalTemplateUseCase.createTemplate(request.toCommand(authUser.userId()));
        ApprovalTemplateCreateResponse data = ApprovalTemplateCreateResponse.from(templateId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(ApprovalResponseCode.TEMPLATE_CREATED, data));
    }

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<ApprovalTemplateSummaryResponse>>> getTemplates() {
        List<ApprovalTemplateSummaryResponse> responses = approvalTemplateQueryUseCase.getTemplates().stream()
                .map(ApprovalTemplateSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.TEMPLATE_LIST_RETRIEVED, responses));
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<GlobalApiResponse<ApprovalTemplateDetailResponse>> getTemplateDetail(@PathVariable Long templateId) {
        ApprovalTemplateDetailView view = approvalTemplateQueryUseCase.getTemplateDetail(templateId);
        ApprovalTemplateDetailResponse data = ApprovalTemplateDetailResponse.from(view);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.TEMPLATE_DETAIL_RETRIEVED, data));
    }

    @PatchMapping("/{templateId}")
    public ResponseEntity<Void> updateTemplate(@PathVariable Long templateId,
                                                @Valid @RequestBody UpdateApprovalTemplateRequest request) {
        updateApprovalTemplateUseCase.updateTemplate(request.toCommand(templateId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        deleteApprovalTemplateUseCase.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }
}
