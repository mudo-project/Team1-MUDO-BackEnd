package com.academy.mudogroupware.sharedfile.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateSharedFolderRequest(
        @Schema(description = "상위 폴더 ID. 생략하면 시스템 루트 바로 아래에 만듭니다.",
                example = "1AbCdEfGhIjKlMnOpQrStUvWxYz")
        String parentId,
        @Schema(description = "생성할 폴더 이름", example = "수업 운영")
        @NotBlank(message = "폴더 이름은 필수입니다.")
        String name
) {
}
