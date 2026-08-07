package com.academy.mudogroupware.users.presentation.api;

import java.util.List;

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
import com.academy.mudogroupware.users.presentation.api.common.AcademyApplicationResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.RejectAcademyApplicationRequest;
import com.academy.mudogroupware.users.presentation.api.response.AcademyApplicationApproveResponse;
import com.academy.mudogroupware.users.presentation.api.response.AcademyApplicationResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academy-applications")
@RequiredArgsConstructor
public class AcademyApplicationController {

    private final ListAcademyApplicationsUseCase listAcademyApplicationsUseCase;
    private final GetAcademyApplicationUseCase getAcademyApplicationUseCase;
    private final ApproveAcademyApplicationUseCase approveAcademyApplicationUseCase;
    private final RejectAcademyApplicationUseCase rejectAcademyApplicationUseCase;

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<AcademyApplicationResponse>>> list() {
        List<AcademyApplicationResponse> data = listAcademyApplicationsUseCase.listApplications().stream()
                .map(AcademyApplicationResponse::from)
                .toList();
        return ResponseEntity.ok(
                GlobalApiResponse.ok(AcademyApplicationResponseCode.ACADEMY_APPLICATION_LIST_FOUND, data));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<GlobalApiResponse<AcademyApplicationResponse>> get(
            @PathVariable Long applicationId) {
        AcademyApplicationResponse data =
                AcademyApplicationResponse.from(getAcademyApplicationUseCase.getApplication(applicationId));
        return ResponseEntity.ok(
                GlobalApiResponse.ok(AcademyApplicationResponseCode.ACADEMY_APPLICATION_DETAIL_FOUND, data));
    }

    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<GlobalApiResponse<AcademyApplicationApproveResponse>> approve(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long applicationId) {
        ApproveAcademyApplicationResult result = approveAcademyApplicationUseCase.approve(
                new ApproveAcademyApplicationCommand(applicationId, authUser.userId()));
        AcademyApplicationApproveResponse data = AcademyApplicationApproveResponse.from(result);
        return ResponseEntity.ok(
                GlobalApiResponse.ok(AcademyApplicationResponseCode.ACADEMY_APPLICATION_APPROVED, data));
    }

    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<Void> reject(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long applicationId,
            @Valid @RequestBody RejectAcademyApplicationRequest request) {
        rejectAcademyApplicationUseCase.reject(request.toCommand(applicationId, authUser.userId()));
        return ResponseEntity.noContent().build();
    }
}
