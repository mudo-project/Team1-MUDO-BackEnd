package com.academy.mudogroupware.approval.presentation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.usecase.ApprovalQueryUseCase;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalTemplateUseCase;
import com.academy.mudogroupware.approval.application.usecase.DecideApprovalLineUseCase;
import com.academy.mudogroupware.approval.presentation.api.request.CreateApprovalTemplateRequest;
import com.academy.mudogroupware.approval.presentation.api.request.DecideApprovalLineRequest;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalCreateResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalDetailResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalSummaryResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final CreateApprovalTemplateUseCase createApprovalTemplateUseCase;
    private final DecideApprovalLineUseCase decideApprovalLineUseCase;
    private final ApprovalQueryUseCase approvalQueryUseCase;

    @PostMapping
    public ResponseEntity<ApprovalCreateResponse> createTemplate(@Valid @RequestBody CreateApprovalTemplateRequest request) {
        Long approvalTemplateId = createApprovalTemplateUseCase.createTemplate(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApprovalCreateResponse.from(approvalTemplateId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ApprovalSummaryResponse>> getMyApprovals(@RequestParam Long userId) {
        List<ApprovalSummaryResponse> responses = approvalQueryUseCase.getMyApprovals(userId).stream()
                .map(ApprovalSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<ApprovalDetailResponse> getApprovalDetail(@PathVariable Long templateId,
                                                                      @RequestParam Long requesterId) {
        ApprovalDetailView view = approvalQueryUseCase.getApprovalDetail(templateId, requesterId);
        return ResponseEntity.ok(ApprovalDetailResponse.from(view));
    }

    @PostMapping("/{templateId}/decide")
    public ResponseEntity<Void> decide(@PathVariable Long templateId,
                                        @Valid @RequestBody DecideApprovalLineRequest request) {
        decideApprovalLineUseCase.decide(request.toCommand(templateId));
        return ResponseEntity.noContent().build();
    }
}
