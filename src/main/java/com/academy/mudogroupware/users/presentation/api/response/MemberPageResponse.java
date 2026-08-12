package com.academy.mudogroupware.users.presentation.api.response;

import java.util.List;
import java.util.function.Function;

import com.academy.mudogroupware.users.application.result.MemberListItem;
import com.academy.mudogroupware.users.application.result.MemberPage;

import io.swagger.v3.oas.annotations.media.Schema;

public record MemberPageResponse<T>(
        @Schema(description = "현재 페이지의 항목 목록") List<T> content,
        @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0") int page,
        @Schema(description = "페이지 크기", example = "20") int size,
        @Schema(description = "전체 항목 개수", example = "42") long totalElements,
        @Schema(description = "전체 페이지 수", example = "3") int totalPages,
        @Schema(description = "첫 페이지 여부", example = "true") boolean first,
        @Schema(description = "마지막 페이지 여부", example = "false") boolean last,
        @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext,
        @Schema(description = "이전 페이지 존재 여부", example = "false") boolean hasPrevious) {

    public static <T> MemberPageResponse<T> from(MemberPage page, Function<MemberListItem, T> mapper) {
        return new MemberPageResponse<>(
                page.content().stream().map(mapper).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.page() == 0,
                !page.hasNext(),
                page.hasNext(),
                page.page() > 0);
    }
}
