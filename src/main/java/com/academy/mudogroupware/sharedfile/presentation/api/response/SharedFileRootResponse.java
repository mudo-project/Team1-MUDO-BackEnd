package com.academy.mudogroupware.sharedfile.presentation.api.response;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;

import io.swagger.v3.oas.annotations.media.Schema;

public record SharedFileRootResponse(
        @Schema(description = "시스템 루트를 지금 사용할 수 있는지 여부", example = "true")
        boolean ready,
        @Schema(description = "시스템 루트의 Drive 폴더 ID. ready가 false면 null입니다. "
                + "생성·업로드 API의 parentId로 그대로 사용할 수 있습니다.",
                example = "1AbCdEfGhIjKlMnOpQrStUvWxYz")
        String rootId
) {

    public static SharedFileRootResponse from(SharedFileRootView view) {
        return new SharedFileRootResponse(view.ready(), view.rootId());
    }
}
