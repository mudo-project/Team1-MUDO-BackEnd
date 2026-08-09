package com.academy.mudogroupware.file.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.file.application.usecase.GeneratePresignedUploadUrlUseCase;
import com.academy.mudogroupware.file.application.usecase.GetFileDownloadUrlUseCase;
import com.academy.mudogroupware.file.application.usecase.RegisterFileUseCase;
import com.academy.mudogroupware.file.presentation.api.common.FileResponseCode;
import com.academy.mudogroupware.file.presentation.api.request.GeneratePresignedUploadUrlRequest;
import com.academy.mudogroupware.file.presentation.api.request.RegisterFileRequest;
import com.academy.mudogroupware.file.presentation.api.response.FileDownloadUrlResponse;
import com.academy.mudogroupware.file.presentation.api.response.PresignedUploadUrlResponse;
import com.academy.mudogroupware.file.presentation.api.response.RegisterFileResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "파일", description = "S3 첨부파일 업로드/등록/다운로드 URL 조회 API. 특정 도메인(공지, 결재 등) 종속 없이 공용으로 쓰인다.")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final GeneratePresignedUploadUrlUseCase generatePresignedUploadUrlUseCase;
    private final RegisterFileUseCase registerFileUseCase;
    private final GetFileDownloadUrlUseCase getFileDownloadUrlUseCase;

    @Operation(summary = "업로드용 presigned URL 발급",
            description = "받은 objectKey로 S3에 파일을 직접 PUT 업로드할 수 있는 URL을 발급한다. "
                    + "이 시점엔 아직 파일 메타데이터가 생성되지 않는다. 업로드 완료 후 파일 등록 API를 호출해야 fileId가 발급된다.")
    @PostMapping("/presigned-url")
    public ResponseEntity<GlobalApiResponse<PresignedUploadUrlResponse>> generatePresignedUploadUrl(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody GeneratePresignedUploadUrlRequest request) {
        PresignedUploadUrlResponse data = PresignedUploadUrlResponse.from(
                generatePresignedUploadUrlUseCase.generate(request.toCommand(authUser.academyId())));
        return ResponseEntity.ok(GlobalApiResponse.ok(FileResponseCode.PRESIGNED_URL_GENERATED, data));
    }

    @Operation(summary = "파일 메타데이터 등록",
            description = "presigned URL로 S3 업로드를 완료한 뒤 호출한다. 응답으로 받은 fileId를 다른 기능(결재 신청, "
                    + "공지 첨부 등)에서 첨부파일 참조용으로 사용한다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<RegisterFileResponse>> registerFile(
            @Valid @RequestBody RegisterFileRequest request) {
        Long fileId = registerFileUseCase.register(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(FileResponseCode.FILE_REGISTERED, RegisterFileResponse.from(fileId)));
    }

    @Operation(summary = "다운로드용 URL 조회", description = "fileId로 등록된 파일의 임시 다운로드 URL을 조회한다.")
    @GetMapping("/{fileId}/download-url")
    public ResponseEntity<GlobalApiResponse<FileDownloadUrlResponse>> getDownloadUrl(@PathVariable Long fileId) {
        String downloadUrl = getFileDownloadUrlUseCase.getDownloadUrl(fileId);
        return ResponseEntity.ok(GlobalApiResponse.ok(FileResponseCode.DOWNLOAD_URL_RETRIEVED,
                FileDownloadUrlResponse.from(downloadUrl)));
    }
}
