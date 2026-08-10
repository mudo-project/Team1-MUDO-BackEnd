package com.academy.mudogroupware.notice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.notice.application.port.AuthorInfo;
import com.academy.mudogroupware.notice.application.port.NoticeAuthorDirectoryPort;
import com.academy.mudogroupware.notice.domain.exception.NoticeErrorCode;
import com.academy.mudogroupware.notice.domain.exception.NoticeException;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

class PinNoticeServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 9, 0);

    private final NoticeRepository noticeRepository = mock(NoticeRepository.class);
    private final NoticeAuthorDirectoryPort noticeAuthorDirectoryPort = mock(NoticeAuthorDirectoryPort.class);
    private final PinNoticeService service = new PinNoticeService(noticeRepository, noticeAuthorDirectoryPort);

    private Notice noticeOfAcademy(Long academyId) {
        return Notice.create(academyId, 7L, "제목", "내용", false, List.of(), NOW);
    }

    @Test
    void pinSucceedsWhenRequesterIsSameAcademyEvenIfNotAuthor() {
        Notice notice = noticeOfAcademy(1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(noticeAuthorDirectoryPort.getAuthor(99L)).thenReturn(new AuthorInfo(99L, "공지관리자", "STAFF", 1L));

        service.pin(1L, 99L);

        assertThat(notice.isPinned()).isTrue();
        verify(noticeRepository).save(notice);
    }

    @Test
    void unpinSucceedsWhenRequesterIsSameAcademy() {
        Notice notice = noticeOfAcademy(1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(noticeAuthorDirectoryPort.getAuthor(99L)).thenReturn(new AuthorInfo(99L, "다른직원", "STAFF", 1L));

        service.unpin(1L, 99L);

        assertThat(notice.isPinned()).isFalse();
        verify(noticeRepository).save(notice);
    }

    @Test
    void unpinRejectsCrossAcademyRequester() {
        Notice notice = noticeOfAcademy(1L);
        when(noticeRepository.findById(1L)).thenReturn(Optional.of(notice));
        when(noticeAuthorDirectoryPort.getAuthor(99L)).thenReturn(new AuthorInfo(99L, "다른학원직원", "STAFF", 2L));

        assertThatThrownBy(() -> service.unpin(1L, 99L))
                .isInstanceOf(NoticeException.class)
                .extracting(e -> ((NoticeException) e).getErrorCode())
                .isEqualTo(NoticeErrorCode.CROSS_ACADEMY_NOTICE);

        verify(noticeRepository, never()).save(any());
    }

    @Test
    void unpinThrowsWhenNoticeNotFound() {
        when(noticeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unpin(1L, 99L))
                .isInstanceOf(NoticeException.class)
                .extracting(e -> ((NoticeException) e).getErrorCode())
                .isEqualTo(NoticeErrorCode.NOTICE_NOT_FOUND);
    }
}
