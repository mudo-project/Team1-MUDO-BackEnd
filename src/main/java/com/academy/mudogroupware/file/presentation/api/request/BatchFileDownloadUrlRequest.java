package com.academy.mudogroupware.file.presentation.api.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BatchFileDownloadUrlRequest(
        @Schema(description = "다운로드 URL을 조회할 파일 ID 목록. 최대 100개.", example = "[1, 2, 3]")
        @NotEmpty
        @Size(max = 100, message = "한 번에 조회할 수 있는 파일 ID는 최대 100개입니다.")
        List<@NotNull @Positive Long> fileIds
) {
}
