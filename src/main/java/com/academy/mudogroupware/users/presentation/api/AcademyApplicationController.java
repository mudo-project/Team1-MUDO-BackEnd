package com.academy.mudogroupware.users.presentation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.users.application.command.ApproveAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;
import com.academy.mudogroupware.users.application.usecase.ApproveAcademyApplicationUseCase;
import com.academy.mudogroupware.users.application.usecase.GetAcademyApplicationUseCase;
import com.academy.mudogroupware.users.application.usecase.ListAcademyApplicationsUseCase;
import com.academy.mudogroupware.users.application.usecase.RejectAcademyApplicationUseCase;
import com.academy.mudogroupware.users.application.usecase.SubmitAcademyApplicationUseCase;
import com.academy.mudogroupware.users.presentation.api.common.AcademyApplicationResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.RejectAcademyApplicationRequest;
import com.academy.mudogroupware.users.presentation.api.request.SubmitAcademyApplicationRequest;
import com.academy.mudogroupware.users.presentation.api.response.AcademyApplicationApproveResponse;
import com.academy.mudogroupware.users.presentation.api.response.AcademyApplicationResponse;
import com.academy.mudogroupware.users.presentation.api.response.AcademyApplicationSubmitResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "학원 신청", description = "학원 신청 접수(공개)/목록·상세 조회/승인·반려(PLATFORM:SUPER_ADMIN 전용)")
@RestController
@RequestMapping("/api/academy-applications")
@RequiredArgsConstructor
public class AcademyApplicationController {

    private final SubmitAcademyApplicationUseCase submitAcademyApplicationUseCase;
    private final ListAcademyApplicationsUseCase listAcademyApplicationsUseCase;
    private final GetAcademyApplicationUseCase getAcademyApplicationUseCase;
    private final ApproveAcademyApplicationUseCase approveAcademyApplicationUseCase;
    private final RejectAcademyApplicationUseCase rejectAcademyApplicationUseCase;

    @Operation(
            summary = "학원 신청 접수",
            description = "학원이 계정 없이 신청서를 제출합니다. 인증이 필요 없는 공개 API입니다. "
                    + "사업자등록증 등 서류 검증은 이번 스코프에서 제외되었으며, SUPER ADMIN이 승인 단계에서 별도로 확인합니다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<AcademyApplicationSubmitResponse>> submit(
            @Valid @RequestBody SubmitAcademyApplicationRequest request) {
        Long applicationId = submitAcademyApplicationUseCase.submit(request.toCommand());
        AcademyApplicationSubmitResponse data = AcademyApplicationSubmitResponse.from(applicationId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(AcademyApplicationResponseCode.ACADEMY_APPLICATION_SUBMITTED, data));
    }

    @Operation(
            summary = "학원 신청 목록 조회",
            description = "SUPER ADMIN이 들어온 학원 신청 목록을 조회합니다. 페이지네이션이 없습니다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<AcademyApplicationResponse>>> list() {
        List<AcademyApplicationResponse> data = listAcademyApplicationsUseCase.listApplications().stream()
                .map(AcademyApplicationResponse::from)
                .toList();
        return ResponseEntity.ok(
                GlobalApiResponse.ok(AcademyApplicationResponseCode.ACADEMY_APPLICATION_LIST_FOUND, data));
    }

    @Operation(
            summary = "학원 신청 상세 조회",
            description = "신청서 하나의 상세 정보를 조회합니다.")
    @GetMapping("/{applicationId}")
    public ResponseEntity<GlobalApiResponse<AcademyApplicationResponse>> get(
            @Parameter(description = "학원 신청 ID") @PathVariable Long applicationId) {
        AcademyApplicationResponse data =
                AcademyApplicationResponse.from(getAcademyApplicationUseCase.getApplication(applicationId));
        return ResponseEntity.ok(
                GlobalApiResponse.ok(AcademyApplicationResponseCode.ACADEMY_APPLICATION_DETAIL_FOUND, data));
    }

    @Operation(
            summary = "학원 신청 승인",
            description = "신청을 승인합니다. 승인 시 학원과 최초 관리자(원장) 계정이 함께 생성되며, 임시 비밀번호가 응답에 1회 평문으로 포함됩니다.")
    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<GlobalApiResponse<AcademyApplicationApproveResponse>> approve(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "학원 신청 ID") @PathVariable Long applicationId) {
        ApproveAcademyApplicationResult result = approveAcademyApplicationUseCase.approve(
                new ApproveAcademyApplicationCommand(applicationId, authUser.userId()));
        AcademyApplicationApproveResponse data = AcademyApplicationApproveResponse.from(result);
        return ResponseEntity.ok(
                GlobalApiResponse.ok(AcademyApplicationResponseCode.ACADEMY_APPLICATION_APPROVED, data));
    }

    @Operation(
            summary = "학원 신청 반려",
            description = "사유를 남기고 신청을 반려합니다. 이미 승인/반려된 신청서는 다시 처리할 수 없습니다.")
    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "학원 신청 ID") @PathVariable Long applicationId,
            @Valid @RequestBody RejectAcademyApplicationRequest request) {
        rejectAcademyApplicationUseCase.reject(request.toCommand(applicationId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }
}
