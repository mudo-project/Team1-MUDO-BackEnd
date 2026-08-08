package com.academy.mudogroupware.approval.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

import com.academy.mudogroupware.approval.application.command.CancelApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.command.HideApprovalHistoryCommand;
import com.academy.mudogroupware.approval.application.command.ResubmitApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.command.SummarizeApprovalAttachmentCommand;
import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentSummaryView;
import com.academy.mudogroupware.approval.application.query.ApprovalDetailView;
import com.academy.mudogroupware.approval.application.usecase.ApprovalQueryUseCase;
import com.academy.mudogroupware.approval.application.usecase.CancelApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.application.usecase.CreateApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.application.usecase.DecideApprovalLineUseCase;
import com.academy.mudogroupware.approval.application.usecase.HideApprovalHistoryUseCase;
import com.academy.mudogroupware.approval.application.usecase.ResubmitApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.application.usecase.SummarizeApprovalAttachmentUseCase;
import com.academy.mudogroupware.approval.application.usecase.UpdateApprovalDocumentLinesUseCase;
import com.academy.mudogroupware.approval.presentation.api.common.ApprovalResponseCode;
import com.academy.mudogroupware.approval.presentation.api.request.CreateApprovalDocumentRequest;
import com.academy.mudogroupware.approval.presentation.api.request.DecideApprovalLineRequest;
import com.academy.mudogroupware.approval.presentation.api.request.UpdateApprovalDocumentLinesRequest;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalAttachmentSummaryResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalCreateResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalDetailResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalPendingCountResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalSubmittedSummaryResponse;
import com.academy.mudogroupware.approval.presentation.api.response.ApprovalSummaryResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "결재 문서", description = "결재 신청, 조회, 승인/반려, 재상신, 취소, 이력 관리, 첨부파일 AI 요약 API")
@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private static final String APPROVAL_READ_ALL = "APPROVAL:READ_ALL";

    private final CreateApprovalDocumentUseCase createApprovalDocumentUseCase;
    private final DecideApprovalLineUseCase decideApprovalLineUseCase;
    private final UpdateApprovalDocumentLinesUseCase updateApprovalDocumentLinesUseCase;
    private final ResubmitApprovalDocumentUseCase resubmitApprovalDocumentUseCase;
    private final SummarizeApprovalAttachmentUseCase summarizeApprovalAttachmentUseCase;
    private final CancelApprovalDocumentUseCase cancelApprovalDocumentUseCase;
    private final HideApprovalHistoryUseCase hideApprovalHistoryUseCase;
    private final ApprovalQueryUseCase approvalQueryUseCase;

    @Operation(summary = "결재 신청", description = "템플릿을 선택해 결재를 신청한다. 다중 파일 첨부가 가능하다.")
    @PreAuthorize("hasAuthority('APPROVAL:SUBMIT')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<ApprovalCreateResponse>> createDocument(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateApprovalDocumentRequest request) {
        Long documentId = createApprovalDocumentUseCase.createDocument(request.toCommand(authUser.userId()));
        ApprovalCreateResponse data = ApprovalCreateResponse.from(documentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(ApprovalResponseCode.DOCUMENT_CREATED, data));
    }

    @Operation(summary = "전체 결재 목록 조회", description = "권한자가 소속 학원의 전체 결재 문서 목록을 페이지 단위로 조회한다.")
    @GetMapping
    @PreAuthorize("hasAuthority('APPROVAL:READ_ALL')")
    public ResponseEntity<GlobalApiResponse<SliceResponse<ApprovalSubmittedSummaryResponse>>> getAllApprovals(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SliceResponse<ApprovalSubmittedSummaryResponse> data = SliceResponse.from(
                approvalQueryUseCase.getAllApprovals(authUser.academyId(), page, size),
                ApprovalSubmittedSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.ALL_APPROVALS_RETRIEVED, data));
    }

    @Operation(summary = "내게 온 결재 목록 조회", description = "내가 결재선에 포함된 결재 문서 목록을 페이지 단위로 조회한다.")
    @GetMapping("/me")
    public ResponseEntity<GlobalApiResponse<SliceResponse<ApprovalSummaryResponse>>> getMyApprovals(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SliceResponse<ApprovalSummaryResponse> data = SliceResponse.from(
                approvalQueryUseCase.getMyApprovals(authUser.userId(), page, size), ApprovalSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.MY_APPROVALS_RETRIEVED, data));
    }

    @Operation(summary = "내 결재 이력 조회", description = "내가 승인 또는 반려 처리한 결재 문서 목록을 조회한다.")
    @GetMapping("/me/history")
    public ResponseEntity<GlobalApiResponse<SliceResponse<ApprovalSummaryResponse>>> getMyApprovalHistory(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SliceResponse<ApprovalSummaryResponse> data = SliceResponse.from(
                approvalQueryUseCase.getMyApprovalHistory(authUser.userId(), page, size),
                ApprovalSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.MY_APPROVAL_HISTORY_RETRIEVED, data));
    }

    @Operation(summary = "내 결재 이력 삭제", description = "내가 이미 승인 또는 반려한 결재 문서를 내 이력 목록에서만 숨긴다.")
    @DeleteMapping("/me/history/{documentId}")
    public ResponseEntity<Void> hideMyApprovalHistory(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long documentId) {
        hideApprovalHistoryUseCase.hide(new HideApprovalHistoryCommand(documentId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "내가 신청한 결재 목록 조회", description = "내가 신청자인 결재 문서 목록을 페이지 단위로 조회한다.")
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

    @Operation(summary = "결재 대기 건수 조회", description = "내가 현재 결재 차례인 진행 중 결재 건수만 조회한다.")
    @GetMapping("/me/pending-count")
    public ResponseEntity<GlobalApiResponse<ApprovalPendingCountResponse>> getMyPendingCount(
            @AuthenticationPrincipal AuthUser authUser) {
        long count = approvalQueryUseCase.getMyPendingCount(authUser.userId());
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.PENDING_COUNT_RETRIEVED,
                ApprovalPendingCountResponse.from(count)));
    }

    @Operation(summary = "결재 상세 조회", description = "신청자, 결재선 참여자, 또는 전체 조회 권한자가 결재 문서를 상세 조회한다.")
    @GetMapping("/{documentId}")
    public ResponseEntity<GlobalApiResponse<ApprovalDetailResponse>> getApprovalDetail(
            @AuthenticationPrincipal AuthUser authUser,
            Authentication authentication,
            @PathVariable Long documentId) {
        ApprovalDetailView view = approvalQueryUseCase.getApprovalDetail(documentId, authUser.userId(),
                authUser.academyId(), hasApprovalReadAll(authentication));
        ApprovalDetailResponse data = ApprovalDetailResponse.from(view);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.DOCUMENT_DETAIL_RETRIEVED, data));
    }

    @Operation(summary = "결재선 수정", description = "신청자 본인만 가능하다. 이미 승인/반려가 하나라도 처리된 건은 수정할 수 없다.")
    @PatchMapping("/{documentId}/lines")
    public ResponseEntity<Void> updateLines(@AuthenticationPrincipal AuthUser authUser,
                                             @PathVariable Long documentId,
                                             @Valid @RequestBody UpdateApprovalDocumentLinesRequest request) {
        updateApprovalDocumentLinesUseCase.updateLines(request.toCommand(documentId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "결재 승인/반려", description = "현재 차례의 결재자만 승인 또는 반려할 수 있다.")
    @PostMapping("/{documentId}/decide")
    public ResponseEntity<Void> decide(@AuthenticationPrincipal AuthUser authUser,
                                        @PathVariable Long documentId,
                                        @Valid @RequestBody DecideApprovalLineRequest request) {
        decideApprovalLineUseCase.decide(request.toCommand(documentId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "결재 신청 취소", description = "신청자 본인이 아직 결재 처리가 시작되지 않은 진행 중 문서를 취소한다.")
    @PostMapping("/{documentId}/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal AuthUser authUser,
                                        @PathVariable Long documentId) {
        cancelApprovalDocumentUseCase.cancel(new CancelApprovalDocumentCommand(documentId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "결재 재상신", description = "반려된 문서만 신청자 본인이 재상신할 수 있다. 문서 1건당 1회로 제한한다.")
    @PreAuthorize("hasAuthority('APPROVAL:SUBMIT')")
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

    @Operation(summary = "첨부파일 AI 요약 생성",
            description = "Gemini API를 호출해 첨부파일 요약을 생성한다. 신청자 또는 결재선 참여자만 가능하다.")
    @PostMapping("/{documentId}/attachments/{fileId}/summarize")
    public ResponseEntity<GlobalApiResponse<ApprovalAttachmentSummaryResponse>> summarizeAttachment(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long documentId,
            @PathVariable Long fileId) {
        ApprovalAttachmentSummaryView view = summarizeApprovalAttachmentUseCase.summarize(
                new SummarizeApprovalAttachmentCommand(documentId, fileId, authUser.userId()));
        ApprovalAttachmentSummaryResponse data = ApprovalAttachmentSummaryResponse.from(view);
        return ResponseEntity.ok(GlobalApiResponse.ok(ApprovalResponseCode.ATTACHMENT_SUMMARIZED, data));
    }

    private boolean hasApprovalReadAll(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> APPROVAL_READ_ALL.equals(authority.getAuthority()));
    }
}
