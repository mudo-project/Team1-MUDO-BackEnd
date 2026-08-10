package com.academy.mudogroupware.file.presentation.api.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

public record BatchFileDownloadUrlRequest(
        @Schema(description = "다운로드 URL을 조회할 파일 ID 목록", example = "[1, 2, 3]")
        @NotEmpty List<Long> fileIds
) {
}
