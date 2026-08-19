package com.academy.mudogroupware.notice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notice.application.command.NoticeAttachmentInput;
import com.academy.mudogroupware.notice.application.command.UpdateNoticeCommand;
import com.academy.mudogroupware.notice.domain.exception.NoticeErrorCode;
import com.academy.mudogroupware.notice.domain.exception.NoticeException;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.model.NoticeAttachment;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

class UpdateNoticeServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 19, 10, 0);

    private final FakeNoticeRepository noticeRepository = new FakeNoticeRepository();
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final UpdateNoticeService service = new UpdateNoticeService(noticeRepository, clock);

    @Test
    void updatingWithoutAttachmentsFieldLeavesExistingAttachmentsUntouched() {
        Notice existing = noticeRepository.save(Notice.create(1L, "제목", "내용", false,
                List.of(NoticeAttachment.create(10L, "기존.pdf")), NOW));

        service.updateNotice(new UpdateNoticeCommand(existing.getId(), 1L, "새 제목", "새 내용", null));

        Notice updated = noticeRepository.findById(existing.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("새 제목");
        assertThat(updated.getAttachments()).extracting(NoticeAttachment::getFileId).containsExactly(10L);
    }

    @Test
    void updatingWithAttachmentsFieldReplacesExistingAttachments() {
        Notice existing = noticeRepository.save(Notice.create(1L, "제목", "내용", false,
                List.of(NoticeAttachment.create(10L, "기존.pdf")), NOW));

        service.updateNotice(new UpdateNoticeCommand(existing.getId(), 1L, "제목", "내용",
                List.of(new NoticeAttachmentInput(20L, "새파일.pdf"))));

        Notice updated = noticeRepository.findById(existing.getId()).orElseThrow();
        assertThat(updated.getAttachments()).extracting(NoticeAttachment::getFileId).containsExactly(20L);
    }

    @Test
    void updatingWithEmptyAttachmentsListClearsAttachments() {
        Notice existing = noticeRepository.save(Notice.create(1L, "제목", "내용", false,
                List.of(NoticeAttachment.create(10L, "기존.pdf")), NOW));

        service.updateNotice(new UpdateNoticeCommand(existing.getId(), 1L, "제목", "내용", List.of()));

        Notice updated = noticeRepository.findById(existing.getId()).orElseThrow();
        assertThat(updated.getAttachments()).isEmpty();
    }

    @Test
    void throwsWhenRequesterIsNotAuthor() {
        Notice existing = noticeRepository.save(Notice.create(1L, "제목", "내용", false, List.of(), NOW));

        assertThatThrownBy(() -> service.updateNotice(
                new UpdateNoticeCommand(existing.getId(), 999L, "새 제목", "새 내용", null)))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.NOT_AUTHOR_UPDATE);
    }

    private static final class FakeNoticeRepository implements NoticeRepository {
        private final Map<Long, Notice> notices = new java.util.HashMap<>();
        private long sequence = 1L;

        @Override
        public Notice save(Notice notice) {
            Notice saved = notice.getId() != null
                    ? Notice.restore(notice.getId(), notice.getAuthorUserId(), notice.getTitle(),
                            notice.getContent(), notice.isPinned(), notice.getViewCount(),
                            notice.getAttachments(), notice.getCreatedAt(), notice.getUpdatedAt())
                    : Notice.restore(sequence++, notice.getAuthorUserId(), notice.getTitle(),
                            notice.getContent(), notice.isPinned(), notice.getViewCount(),
                            notice.getAttachments(), notice.getCreatedAt(), notice.getUpdatedAt());
            notices.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Notice> findById(Long id) {
            return Optional.ofNullable(notices.get(id));
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
