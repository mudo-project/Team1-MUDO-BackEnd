package com.academy.mudogroupware.sharedfile.presentation.api.request;

import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceFileType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGoogleWorkspaceFileRequest(
        @Schema(description = "상위 폴더 ID. 시스템 루트 바로 아래에 만들려면 루트 ID를 전달합니다.",
                example = "1AbCdEfGhIjKlMnOpQrStUvWxYz")
        @NotBlank(message = "상위 폴더 ID는 필수입니다.")
        String parentId,
        @Schema(description = "생성할 파일 이름", example = "9월 수업계획")
        @NotBlank(message = "파일 이름은 필수입니다.")
        String name,
        @Schema(description = "생성할 Google Workspace 파일 유형", example = "DOCS")
        @NotNull(message = "파일 유형은 필수입니다.")
        GoogleWorkspaceFileType type
) {
}
