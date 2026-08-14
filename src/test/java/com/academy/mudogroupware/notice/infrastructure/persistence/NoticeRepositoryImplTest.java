package com.academy.mudogroupware.notice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NoticeRepositoryImplTest {

    private final NoticeJpaRepository noticeJpaRepository = Mockito.mock(NoticeJpaRepository.class);
    private final NoticeRepositoryImpl repository = new NoticeRepositoryImpl(noticeJpaRepository);

    @Test
    void findByIdReturnsOnlyActiveNotice() {
        when(noticeJpaRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThat(repository.findById(1L)).isEmpty();

        verify(noticeJpaRepository).findActiveById(1L);
        verify(noticeJpaRepository, never()).findById(1L);
    }
}
