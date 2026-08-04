package com.academy.mudogroupware.notice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NoticeReadRepositoryImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 15, 0);

    private final NoticeReadJpaRepository noticeReadJpaRepository = mock(NoticeReadJpaRepository.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
    private final NoticeReadRepositoryImpl repository = new NoticeReadRepositoryImpl(noticeReadJpaRepository, clock);

    @Test
    void markReadSavesEntityWithClockBasedTimestampWhenNotAlreadyRead() {
        when(noticeReadJpaRepository.existsByNoticeIdAndUserId(1L, 7L)).thenReturn(false);

        repository.markRead(1L, 7L);

        ArgumentCaptor<NoticeReadEntity> captor = ArgumentCaptor.forClass(NoticeReadEntity.class);
        verify(noticeReadJpaRepository).save(captor.capture());
        assertThat(captor.getValue().getNoticeId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getReadAt()).isEqualTo(NOW);
    }

    @Test
    void markReadDoesNothingWhenAlreadyRead() {
        when(noticeReadJpaRepository.existsByNoticeIdAndUserId(1L, 7L)).thenReturn(true);

        repository.markRead(1L, 7L);

        verify(noticeReadJpaRepository, never()).save(any());
    }
}
