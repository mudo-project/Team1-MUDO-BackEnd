package com.academy.mudogroupware.notice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.notice.application.command.CreateNoticeCommand;
import com.academy.mudogroupware.notice.application.command.NoticeAttachmentInput;
import com.academy.mudogroupware.notice.application.port.AuthorInfo;
import com.academy.mudogroupware.notice.application.port.NoticeAuthorDirectoryPort;
import com.academy.mudogroupware.notice.domain.model.Notice;
import com.academy.mudogroupware.notice.domain.repository.NoticeRepository;

class CreateNoticeServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    private final FakeNoticeRepository noticeRepository = new FakeNoticeRepository();
    private final NoticeAuthorDirectoryPort authorDirectoryPort = new NoticeAuthorDirectoryPort() {
        @Override
        public AuthorInfo getAuthor(Long userId) {
            return new AuthorInfo(userId, "김직원", "직원", 10L);
        }

        @Override
        public Map<Long, AuthorInfo> getAuthors(List<Long> userIds) {
            return Map.of();
        }

        @Override
        public long countActiveUsers(Long academyId) {
            return 0;
        }
    };
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul"));
    private final CreateNoticeService service = new CreateNoticeService(noticeRepository, authorDirectoryPort, clock);

    @Test
    void createsNoticeWithAttachmentsReferencingUploadedFileIds() {
        Long noticeId = service.createNotice(new CreateNoticeCommand(
                1L, "이번 달 공지", "본문입니다", false,
                List.of(new NoticeAttachmentInput(5L, "휴가원.pdf"))));

        Notice saved = noticeRepository.findById(noticeId).orElseThrow();
        assertThat(saved.getAcademyId()).isEqualTo(10L);
        assertThat(saved.getAttachments()).hasSize(1);
        assertThat(saved.getAttachments().get(0).getFileId()).isEqualTo(5L);
        assertThat(saved.getAttachments().get(0).getFileName()).isEqualTo("휴가원.pdf");
    }

    @Test
    void createsNoticeWithoutAttachmentsWhenNoneProvided() {
        Long noticeId = service.createNotice(new CreateNoticeCommand(
                1L, "공지", "본문", false, null));

        Notice saved = noticeRepository.findById(noticeId).orElseThrow();
        assertThat(saved.getAttachments()).isEmpty();
    }

    private static final class FakeNoticeRepository implements NoticeRepository {
        private final Map<Long, Notice> notices = new java.util.HashMap<>();
        private long sequence = 1L;

        @Override
        public Notice save(Notice notice) {
            Notice saved = Notice.restore(sequence++, notice.getAcademyId(), notice.getAuthorUserId(),
                    notice.getTitle(), notice.getContent(), notice.isPinned(), notice.getViewCount(),
                    notice.getAttachments(), notice.getCreatedAt(), notice.getUpdatedAt());
            notices.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Notice> findById(Long id) {
            return Optional.ofNullable(notices.get(id));
        }

        @Override
        public PageResult<Notice> findAll(Long academyId, String titleKeyword, int page, int size) {
            return PageResult.of(List.of(), page, size, false);
        }

        @Override
        public void deleteById(Long id) {
            notices.remove(id);
        }
    }
}
