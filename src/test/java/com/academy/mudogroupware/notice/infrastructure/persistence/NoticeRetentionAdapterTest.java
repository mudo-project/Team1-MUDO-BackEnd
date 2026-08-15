package com.academy.mudogroupware.notice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.academy.mudogroupware.notice.application.retention.NoticeAttachmentFileReference;
import com.academy.mudogroupware.notice.application.retention.NoticeRetentionTarget;

class NoticeRetentionAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 14, 3, 0);

    private final NoticeJpaRepository noticeJpaRepository = Mockito.mock(NoticeJpaRepository.class);
    private final NoticeAttachmentJpaRepository noticeAttachmentJpaRepository =
            Mockito.mock(NoticeAttachmentJpaRepository.class);
    private final NoticeReadJpaRepository noticeReadJpaRepository = Mockito.mock(NoticeReadJpaRepository.class);
    private final NoticeRetentionAdapter adapter = new NoticeRetentionAdapter(
            noticeJpaRepository, noticeAttachmentJpaRepository, noticeReadJpaRepository);

    @Test
    void findExpiredNoticeTargetsGroupsFileIdsByNoticeId() {
        when(noticeJpaRepository.findHardDeleteCandidateIds(NOW, 500)).thenReturn(List.of(10L, 11L));
        when(noticeAttachmentJpaRepository.findFileReferencesByNoticeIds(List.of(10L, 11L)))
                .thenReturn(List.of(
                        new NoticeAttachmentFileReference(10L, 1L),
                        new NoticeAttachmentFileReference(10L, 2L),
                        new NoticeAttachmentFileReference(11L, 3L)));

        List<NoticeRetentionTarget> targets = adapter.findExpiredNoticeTargets(NOW, 500);

        assertThat(targets).containsExactly(
                new NoticeRetentionTarget(10L, List.of(1L, 2L)),
                new NoticeRetentionTarget(11L, List.of(3L)));
    }

    @Test
    void findExpiredNoticeTargetsKeepsNoticeWithoutAttachments() {
        when(noticeJpaRepository.findHardDeleteCandidateIds(NOW, 500)).thenReturn(List.of(10L));
        when(noticeAttachmentJpaRepository.findFileReferencesByNoticeIds(List.of(10L))).thenReturn(List.of());

        List<NoticeRetentionTarget> targets = adapter.findExpiredNoticeTargets(NOW, 500);

        assertThat(targets).containsExactly(new NoticeRetentionTarget(10L, List.of()));
    }

    @Test
    void findExpiredNoticeTargetsReturnsEmptyWithoutLoadingAttachments() {
        when(noticeJpaRepository.findHardDeleteCandidateIds(NOW, 500)).thenReturn(List.of());

        List<NoticeRetentionTarget> targets = adapter.findExpiredNoticeTargets(NOW, 500);

        assertThat(targets).isEmpty();
        verifyNoInteractions(noticeAttachmentJpaRepository);
    }
}
