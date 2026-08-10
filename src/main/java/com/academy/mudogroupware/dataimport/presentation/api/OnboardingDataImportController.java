package com.academy.mudogroupware.dataimport.presentation.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.academy.mudogroupware.dataimport.application.command.CreateOnboardingImportCommand;
import com.academy.mudogroupware.dataimport.application.port.ImportFile;
import com.academy.mudogroupware.dataimport.application.usecase.ConfirmOnboardingImportUseCase;
import com.academy.mudogroupware.dataimport.application.usecase.CreateOnboardingImportUseCase;
import com.academy.mudogroupware.dataimport.application.usecase.GetImportDraftUseCase;
import com.academy.mudogroupware.dataimport.application.usecase.GetImportResultUseCase;
import com.academy.mudogroupware.dataimport.application.usecase.UpdateImportDraftUseCase;
import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;
import com.academy.mudogroupware.dataimport.domain.model.ImportResult;
import com.academy.mudogroupware.dataimport.presentation.api.common.DataImportResponseCode;
import com.academy.mudogroupware.dataimport.presentation.api.request.UpdateImportDraftRequest;
import com.academy.mudogroupware.dataimport.presentation.api.response.ImportCreatedResponse;
import com.academy.mudogroupware.dataimport.presentation.api.response.ImportDraftResponse;
import com.academy.mudogroupware.dataimport.presentation.api.response.ImportResultResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "데이터 가져오기", description = "학생/강의/수강 관계 초기 데이터 가져오기 API")
@RestController
@RequestMapping("/api/data-imports/onboarding")
@RequiredArgsConstructor
public class OnboardingDataImportController {

    private static final String IMPORT_PERMISSION =
            "hasAuthority('STUDENT:MANAGE') and hasAuthority('LECTURE:MANAGE')";

    private final CreateOnboardingImportUseCase createOnboardingImportUseCase;
    private final GetImportDraftUseCase getImportDraftUseCase;
    private final UpdateImportDraftUseCase updateImportDraftUseCase;
    private final ConfirmOnboardingImportUseCase confirmOnboardingImportUseCase;
    private final GetImportResultUseCase getImportResultUseCase;

    @Operation(summary = "초기 데이터 파일 업로드", description = "학생/강의/수강 관계 파일을 분석해 등록 초안을 생성한다.")
    @PreAuthorize(IMPORT_PERMISSION)
    @PostMapping(value = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalApiResponse<ImportCreatedResponse>> uploadFiles(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestPart(required = false) MultipartFile studentFile,
            @RequestPart(required = false) MultipartFile lectureFile,
            @RequestPart(required = false) MultipartFile enrollmentFile) throws IOException {
        Long importId = createOnboardingImportUseCase.create(new CreateOnboardingImportCommand(
                authUser.academyId(),
                authUser.userId(),
                toImportFiles(studentFile, lectureFile, enrollmentFile)));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(DataImportResponseCode.IMPORT_CREATED,
                        ImportCreatedResponse.from(importId)));
    }

    @Operation(summary = "가져오기 초안 조회", description = "학생/강의/수강 등록 후보를 탭별로 조회한다.")
    @PreAuthorize(IMPORT_PERMISSION)
    @GetMapping("/{importId}/draft")
    public ResponseEntity<GlobalApiResponse<ImportDraftResponse>> getDraft(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long importId) {
        ImportDraft draft = getImportDraftUseCase.getDraft(authUser.academyId(), importId);
        return ResponseEntity.ok(GlobalApiResponse.ok(DataImportResponseCode.DRAFT_RETRIEVED,
                ImportDraftResponse.from(draft)));
    }

    @Operation(summary = "가져오기 초안 수정", description = "검토 화면에서 수정/제외한 초안을 전체 교체한다.")
    @PreAuthorize(IMPORT_PERMISSION)
    @PatchMapping("/{importId}/draft")
    public ResponseEntity<Void> updateDraft(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long importId,
            @Valid @RequestBody UpdateImportDraftRequest request) {
        updateImportDraftUseCase.updateDraft(request.toCommand(authUser.academyId(), importId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "가져오기 확정", description = "선택된 등록 가능 후보를 실제 학생/강의/수강 데이터로 저장한다.")
    @PreAuthorize(IMPORT_PERMISSION)
    @PostMapping("/{importId}/confirm")
    public ResponseEntity<GlobalApiResponse<ImportResultResponse>> confirm(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long importId) {
        ImportResult result = confirmOnboardingImportUseCase.confirm(authUser.academyId(), importId,
                authUser.userId());
        return ResponseEntity.ok(GlobalApiResponse.ok(DataImportResponseCode.IMPORT_CONFIRMED,
                ImportResultResponse.from(result)));
    }

    @Operation(summary = "가져오기 결과 조회", description = "확정 저장 결과를 조회한다.")
    @PreAuthorize(IMPORT_PERMISSION)
    @GetMapping("/{importId}/result")
    public ResponseEntity<GlobalApiResponse<ImportResultResponse>> getResult(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long importId) {
        ImportResult result = getImportResultUseCase.getResult(authUser.academyId(), importId);
        return ResponseEntity.ok(GlobalApiResponse.ok(DataImportResponseCode.RESULT_RETRIEVED,
                ImportResultResponse.from(result)));
    }

    private List<ImportFile> toImportFiles(MultipartFile studentFile, MultipartFile lectureFile,
                                           MultipartFile enrollmentFile) throws IOException {
        List<ImportFile> files = new ArrayList<>();
        if (studentFile != null && !studentFile.isEmpty()) {
            files.add(ImportFile.student(studentFile.getOriginalFilename(), studentFile.getBytes()));
        }
        if (lectureFile != null && !lectureFile.isEmpty()) {
            files.add(ImportFile.lecture(lectureFile.getOriginalFilename(), lectureFile.getBytes()));
        }
        if (enrollmentFile != null && !enrollmentFile.isEmpty()) {
            files.add(ImportFile.enrollment(enrollmentFile.getOriginalFilename(), enrollmentFile.getBytes()));
        }
        return files;
    }
}
