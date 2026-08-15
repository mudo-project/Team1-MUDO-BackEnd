package com.academy.mudogroupware.attendance.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class AttendanceCorrectionRequestRepositoryImplPaginationTest {

    @Test
    void returnsTotalPageMetadataFromJpaPage() {
        AttendanceCorrectionRequestJpaRepository repository = mock(AttendanceCorrectionRequestJpaRepository.class);
        when(repository.findPage(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 42));

        var result = new AttendanceCorrectionRequestRepositoryImpl(repository)
                .findAll(null, 1, 20);

        assertThat(result.totalElements()).isEqualTo(42);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }
}
