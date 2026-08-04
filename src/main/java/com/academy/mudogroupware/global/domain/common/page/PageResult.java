package com.academy.mudogroupware.global.domain.common.page;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> content, int page, int size, boolean hasNext) {

    public static <T> PageResult<T> of(List<T> content, int page, int size, boolean hasNext) {
        return new PageResult<>(content, page, size, hasNext);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, hasNext);
    }
}
