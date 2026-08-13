package com.academy.mudogroupware.global.presentation.api.common;

import java.util.List;
import java.util.function.Function;

import com.academy.mudogroupware.global.domain.common.page.PagedResult;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious) {

    public static <T, R> PageResponse<R> from(PagedResult<T> result, Function<T, R> mapper) {
        return new PageResponse<>(result.content().stream().map(mapper).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(),
                result.first(), result.last(), result.hasNext(), result.hasPrevious());
    }
}
