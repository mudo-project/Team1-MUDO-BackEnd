package com.academy.mudogroupware.global.domain.common.page;

import java.util.List;
import java.util.function.Function;

public record PagedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious) {

    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean hasNext = page < totalPages - 1;
        return new PagedResult<>(content, page, size, totalElements, totalPages,
                page == 0, !hasNext, hasNext, page > 0);
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        return new PagedResult<>(content.stream().map(mapper).toList(), page, size,
                totalElements, totalPages, first, last, hasNext, hasPrevious);
    }
}
