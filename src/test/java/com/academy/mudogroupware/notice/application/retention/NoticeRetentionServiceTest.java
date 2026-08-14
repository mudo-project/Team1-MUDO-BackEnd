package com.academy.mudogroupware.notice.application.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.global.scheduler.RetentionJobResult;
import com.academy.mudogroupware.notice.application.port.NoticeRetentionPort;
import com.academy.mudogroupware.notice.domain.event.NoticeAttachmentFilesCleanupRequestedEvent;

class NoticeRetentionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 14, 3, 0);

    private final FakeNoticeRetentionPort port = new FakeNoticeRetentionPort();
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @Test
    void returnsEmptyResultWhenNoExpiredNotices() {
        NoticeRetentionService service = new NoticeRetentionService(
                new NoticeRetentionProperties(30, 500), port, eventPublisher);

        RetentionJobResult result = service.hardDeleteExpiredNotices(NOW);

        assertThat(result).isEqualTo(RetentionJobResult.empty(NoticeRetentionService.JOB_NAME));
        assertThat(port.invocations).isEmpty();
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deletesNoticeChildrenBeforeNoticeAndRequestsFileCleanup() {
        port.targets = List.of(
                new NoticeRetentionTarget(10L, List.of(1L, 2L)),
                new NoticeRetentionTarget(11L, List.of(2L, 3L)));
        port.readDeleteCount = 4;
        port.attachmentDeleteCount = 3;
        port.noticeDeleteCount = 2;
        NoticeRetentionService service = new NoticeRetentionService(
                new NoticeRetentionProperties(30, 500), port, eventPublisher);

        RetentionJobResult result = service.hardDeleteExpiredNotices(NOW);

        assertThat(result).isEqualTo(new RetentionJobResult(NoticeRetentionService.JOB_NAME, 2, 7, 2));
        assertThat(port.invocations).containsExactly(
                "deleteReads:[10, 11]",
                "deleteAttachments:[10, 11]",
                "hardDeleteNotices:[10, 11]");
        verify(eventPublisher).publishEvent(
                new NoticeAttachmentFilesCleanupRequestedEvent(List.of(1L, 2L, 3L)));
    }

    @Test
    void passesNowAndBatchSizeToCandidateLookup() {
        NoticeRetentionService service = new NoticeRetentionService(
                new NoticeRetentionProperties(7, 200), port, eventPublisher);

        service.hardDeleteExpiredNotices(NOW);

        assertThat(port.lastNow).isEqualTo(NOW);
        assertThat(port.lastBatchSize).isEqualTo(200);
    }

    private static final class FakeNoticeRetentionPort implements NoticeRetentionPort {
        private List<NoticeRetentionTarget> targets = List.of();
        private LocalDateTime lastNow;
        private int lastBatchSize;
        private int readDeleteCount;
        private int attachmentDeleteCount;
        private int noticeDeleteCount;
        private final List<String> invocations = new ArrayList<>();

        @Override
        public List<NoticeRetentionTarget> findExpiredNoticeTargets(LocalDateTime now, int batchSize) {
            this.lastNow = now;
            this.lastBatchSize = batchSize;
            return targets;
        }

        @Override
        public int deleteReadRecordsByNoticeIds(List<Long> noticeIds) {
            invocations.add("deleteReads:" + noticeIds);
            return readDeleteCount;
        }

        @Override
        public int deleteAttachmentsByNoticeIds(List<Long> noticeIds) {
            invocations.add("deleteAttachments:" + noticeIds);
            return attachmentDeleteCount;
        }

        @Override
        public int hardDeleteNoticesByIds(List<Long> noticeIds, LocalDateTime now) {
            invocations.add("hardDeleteNotices:" + noticeIds);
            return noticeDeleteCount;
        }
    }
}
