package com.academy.mudogroupware.sharedfile.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;

import io.swagger.v3.oas.annotations.media.Schema;

public record SharedFileItemResponse(
        @Schema(description = "Google Drive 파일·폴더 ID", example = "1AbCdEfGhIjKlMnOpQrStUvWxYz")
        String id,
        @Schema(description = "파일·폴더 이름", example = "9월 수업계획.docx")
        String name,
        @Schema(description = "Google Drive MIME type", example = "application/vnd.google-apps.document")
        String mimeType,
        @Schema(description = "Google 새 탭 미리보기 URL", example = "https://drive.google.com/file/d/xxx/view")
        String viewUrl,
        @Schema(description = "다운로드 가능 여부", example = "true")
        boolean downloadable,
        @Schema(description = "마지막 수정 시각")
        LocalDateTime modifiedAt
) {

    public static SharedFileItemResponse from(SharedFileItemView view) {
        return new SharedFileItemResponse(
                view.id(), view.name(), view.mimeType(), view.viewUrl(), view.downloadable(), view.modifiedAt());
    }
}
