package com.academy.mudogroupware.global.presentation.api.common;

import java.util.List;
import java.util.function.Function;

import com.academy.mudogroupware.global.domain.common.page.PageResult;

public record SliceResponse<T>(List<T> content, int page, int size, boolean hasNext) {

    public static <T, R> SliceResponse<R> from(PageResult<T> pageResult, Function<T, R> mapper) {
        return new SliceResponse<>(
                pageResult.content().stream().map(mapper).toList(),
                pageResult.page(),
                pageResult.size(),
                pageResult.hasNext());
    }
}
