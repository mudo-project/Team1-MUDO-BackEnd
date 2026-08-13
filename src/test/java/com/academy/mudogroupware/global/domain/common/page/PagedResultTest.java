package com.academy.mudogroupware.global.domain.common.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PagedResultTest {

    @Test
    void calculatesPageMetadataFromTotalElements() {
        PagedResult<String> result = PagedResult.of(List.of("a", "b"), 0, 20, 42);

        assertThat(result.totalElements()).isEqualTo(42);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isFalse();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    void marksLastPageAndPreviousPage() {
        PagedResult<String> result = PagedResult.of(List.of("a", "b"), 2, 20, 42);

        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void emptyResultHasZeroTotalPages() {
        PagedResult<String> result = PagedResult.of(List.of(), 0, 20, 0);

        assertThat(result.totalPages()).isZero();
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }
}
