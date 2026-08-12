package com.academy.mudogroupware.sharedfile.presentation.api.response;

import java.util.List;

import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;

import io.swagger.v3.oas.annotations.media.Schema;

// Google Drive의 page token 기반 페이지네이션을 그대로 노출한다. 다른 도메인의 offset 기반
// page/size 공통 응답과 달리 total count를 계산하지 않는다.
public record SharedFileItemsResponse(
        List<SharedFileItemResponse> items,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "다음 페이지 조회에 사용할 cursor. hasNext가 false면 null입니다.", example = "CiQAxxxx")
        String nextCursor
) {

    public static SharedFileItemsResponse from(SharedFileItemsView view) {
        List<SharedFileItemResponse> items = view.items().stream()
                .map(SharedFileItemResponse::from)
                .toList();
        return new SharedFileItemsResponse(items, view.hasNext(), view.nextCursor());
    }
}
