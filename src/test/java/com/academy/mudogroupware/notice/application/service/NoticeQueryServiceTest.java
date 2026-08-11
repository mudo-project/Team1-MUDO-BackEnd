package com.academy.mudogroupware.notice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notice.application.port.AuthorInfo;
import com.academy.mudogroupware.notice.application.port.NoticeAuthorDirectoryPort;
import com.academy.mudogroupware.notice.application.query.NoticeSummaryView;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeReadRepository;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

class NoticeQueryServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 7, 10, 0);

    private final NoticeRepository noticeRepository = mock(NoticeRepository.class);
    private final NoticeReadRepository noticeReadRepository = mock(NoticeReadRepository.class);
    private final NoticeAuthorDirectoryPort noticeAuthorDirectoryPort = mock(NoticeAuthorDirectoryPort.class);

    private NoticeQueryService service;

    @BeforeEach
    void setUp() {
        service = new NoticeQueryService(noticeRepository, noticeReadRepository, noticeAuthorDirectoryPort);
    }

    @Test
    void getNoticesResolvesAuthorsAndReadFlagsInBatch() {
        Notice first = Notice.restore(1L, 20L, "First", "Content", false, 0L, List.of(), NOW, NOW);
        Notice second = Notice.restore(2L, 21L, "Second", "Content", true, 0L, List.of(), NOW, NOW);
        when(noticeAuthorDirectoryPort.getAuthor(10L))
                .thenReturn(new AuthorInfo(10L, "Requester", "STAFF"));
        when(noticeRepository.findAll(null, 0, 20))
                .thenReturn(PageResult.of(List.of(first, second), 0, 20, false));
        when(noticeAuthorDirectoryPort.getAuthors(List.of(20L, 21L)))
                .thenReturn(Map.of(
                        20L, new AuthorInfo(20L, "Writer A", "STAFF"),
                        21L, new AuthorInfo(21L, "Writer B", "TEACHER")));
        when(noticeReadRepository.findReadNoticeIds(List.of(1L, 2L), 10L)).thenReturn(Set.of(2L));

        PageResult<NoticeSummaryView> result = service.getNotices(10L, null, 0, 20);

        assertThat(result.content()).extracting(NoticeSummaryView::authorName)
                .containsExactly("Writer A", "Writer B");
        assertThat(result.content()).extracting(NoticeSummaryView::read)
                .containsExactly(false, true);
        verify(noticeAuthorDirectoryPort).getAuthors(List.of(20L, 21L));
        verify(noticeReadRepository).findReadNoticeIds(List.of(1L, 2L), 10L);
        verify(noticeAuthorDirectoryPort, never()).getAuthor(20L);
        verify(noticeAuthorDirectoryPort, never()).getAuthor(21L);
        verify(noticeReadRepository, never()).hasRead(1L, 10L);
        verify(noticeReadRepository, never()).hasRead(2L, 10L);
    }
}
