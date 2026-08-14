package com.academy.mudogroupware.notice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notice.application.retention.NoticeRetentionProperties;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

class DeleteNoticeServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 14, 10, 0);

    private final FakeNoticeRepository noticeRepository = new FakeNoticeRepository();
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final DeleteNoticeService service = new DeleteNoticeService(
            noticeRepository, clock, new NoticeRetentionProperties(30, 500));

    @Test
    void deleteNoticeMarksNoticeHiddenUntilRetentionExpires() {
        Notice notice = Notice.restore(1L, 7L, "notice", "content", false, 0L, List.of(), NOW, NOW);
        noticeRepository.notices.put(1L, notice);

        service.deleteNotice(1L, 7L);

        Notice deleted = noticeRepository.notices.get(1L);
        assertThat(deleted.getDeletedAt()).isEqualTo(NOW);
        assertThat(deleted.getRetentionUntil()).isEqualTo(NOW.plusDays(30));
        assertThat(deleted.isDeleted()).isTrue();
    }

    private static final class FakeNoticeRepository implements NoticeRepository {
        private final Map<Long, Notice> notices = new HashMap<>();

        @Override
        public Notice save(Notice notice) {
            notices.put(notice.getId(), notice);
            return notice;
        }

        @Override
        public Optional<Notice> findById(Long id) {
            Notice notice = notices.get(id);
            return notice != null && !notice.isDeleted() ? Optional.of(notice) : Optional.empty();
        }

        @Override
        public PageResult<Notice> findAll(String titleKeyword, int page, int size) {
            return PageResult.of(List.of(), page, size, false);
        }

        @Override
        public void deleteById(Long id) {
            notices.remove(id);
        }
    }
}
