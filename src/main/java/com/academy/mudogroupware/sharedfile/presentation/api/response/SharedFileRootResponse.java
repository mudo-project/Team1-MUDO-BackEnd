package com.academy.mudogroupware.sharedfile.presentation.api.response;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;

import io.swagger.v3.oas.annotations.media.Schema;

public record SharedFileRootResponse(
        @Schema(description = "시스템 루트를 지금 사용할 수 있는지 여부", example = "true")
        boolean ready
) {

    public static SharedFileRootResponse from(SharedFileRootView view) {
        return new SharedFileRootResponse(view.ready());
    }
}
