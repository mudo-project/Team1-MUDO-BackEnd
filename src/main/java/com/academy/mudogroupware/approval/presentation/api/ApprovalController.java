package com.academy.mudogroupware.approval.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.approval.application.command.ResubmitApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.usecase.ApprovalQueryUseCase;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.application.usecase.DecideApprovalLineUseCase;
import com.academy.mudogroupware.approval.application.usecase.ResubmitApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.application.usecase.UpdateApprovalDocumentLinesUseCase;
import com.academy.mudogroupware.approval.presentation.api.common.ApprovalResponseCode;
import com.academy.mudogroupware.approval.presentation.api.request.CreateApprovalDocumentRequest;
import com.academy.mudogroupware.approval.presentation.api.request.DecideApprovalLineRequest;
import com.academy.mudogroupware.approval.presentation.api.request.UpdateApprovalDocumentLinesRequest;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalCreateResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalDetailResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalPendingCountResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalSubmittedSummaryResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalSummaryResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final CreateApprovalDocumentUseCase createApprovalDocumentUseCase;
    private final DecideApprovalLineUseCase decideApprovalLineUseCase;
    private final UpdateApprovalDocumentLinesUseCase updateApprovalDocumentLinesUseCase;
    private final ResubmitApprovalDocumentUseCase resubmitApprovalDocumentUseCase;
    private final ApprovalQueryUseCase approvalQueryUseCase;

    @PostMapping
    public ResponseEntity<GlobalApiResponse<ApprovalCreateResponse>> createDocument(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateApprovalDocumentRequest request) {
        Long documentId = createApprovalDocumentUseCase.createDocument(request.toCommand(authUser.userId()));
        ApprovalCreateResponse data = ApprovalCreateResponse.from(documentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(ApprovalResponseCode.DOCUMENT_CREATED, data));
    }

    @GetMapping("/me")
    public ResponseEntity<GlobalApiResponse<SliceResponse<ApprovalSummaryResponse>>> getMyApprovals(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SliceResponse<ApprovalSummaryResponse> data = SliceResponse.from(
                approvalQueryUseCase.getMyApprovals(authUser.userId(), page, size), ApprovalSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.MY_APPROVALS_RETRIEVED, data));
    }

    @GetMapping("/me/submitted")
    public ResponseEntity<GlobalApiResponse<SliceResponse<ApprovalSubmittedSummaryResponse>>> getMySubmittedApprovals(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SliceResponse<ApprovalSubmittedSummaryResponse> data = SliceResponse.from(
                approvalQueryUseCase.getMySubmittedApprovals(authUser.userId(), page, size),
                ApprovalSubmittedSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.MY_SUBMITTED_APPROVALS_RETRIEVED, data));
    }

    @GetMapping("/me/pending-count")
    public ResponseEntity<GlobalApiResponse<ApprovalPendingCountResponse>> getMyPendingCount(
            @AuthenticationPrincipal AuthUser authUser) {
        long count = approvalQueryUseCase.getMyPendingCount(authUser.userId());
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.PENDING_COUNT_RETRIEVED,
                ApprovalPendingCountResponse.from(count)));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<GlobalApiResponse<ApprovalDetailResponse>> getApprovalDetail(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long documentId) {
        ApprovalDetailView view = approvalQueryUseCase.getApprovalDetail(documentId, authUser.userId());
        ApprovalDetailResponse data = ApprovalDetailResponse.from(view);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.DOCUMENT_DETAIL_RETRIEVED, data));
    }

    @PatchMapping("/{documentId}/lines")
    public ResponseEntity<Void> updateLines(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable Long documentId,
                                             @Valid @RequestBody UpdateApprovalDocumentLinesRequest request) {
        updateApprovalDocumentLinesUseCase.updateLines(request.toCommand(documentId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentId}/decide")
    public ResponseEntity<Void> decide(@AuthenticationPrincipal AuthUser authUser,
                                        @PathVariable Long documentId,
                                        @Valid @RequestBody DecideApprovalLineRequest request) {
        decideApprovalLineUseCase.decide(request.toCommand(documentId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentId}/resubmit")
    public ResponseEntity<GlobalApiResponse<ApprovalCreateResponse>> resubmit(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long documentId) {
        Long newDocumentId = resubmitApprovalDocumentUseCase.resubmit(
                new ResubmitApprovalDocumentCommand(documentId, authUser.userId()));
        ApprovalCreateResponse data = ApprovalCreateResponse.from(newDocumentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(ApprovalResponseCode.DOCUMENT_RESUBMITTED, data));
    }
}
