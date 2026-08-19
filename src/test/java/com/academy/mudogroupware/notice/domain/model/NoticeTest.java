package com.academy.mudogroupware.notice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class NoticeTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 9, 0);

    @Test
    void createUsesGivenNowForCreatedAtAndUpdatedAt() {
        Notice notice = Notice.create(7L, "제목", "내용", false, List.of(), NOW);

        assertThat(notice.getCreatedAt()).isEqualTo(NOW);
        assertThat(notice.getUpdatedAt()).isEqualTo(NOW);
        assertThat(notice.getViewCount()).isZero();
        assertThat(notice.isPinned()).isFalse();
    }

    @Test
    void createRejectsNullNow() {
        assertThatThrownBy(() -> Notice.create(7L, "제목", "내용", false, List.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsBlankTitle() {
        assertThatThrownBy(() -> Notice.create(7L, " ", "내용", false, List.of(), NOW))
                .isInstanceOf(com.academy.mudogroupware.notice.domain.exception.NoticeException.class);
    }

    @Test
    void updateChangesUpdatedAtToGivenNowWithoutTouchingCreatedAt() {
        Notice notice = Notice.create(7L, "제목", "내용", false, List.of(), NOW);
        LocalDateTime updatedNow = NOW.plusHours(2);

        notice.update("새 제목", "새 내용", updatedNow);

        assertThat(notice.getTitle()).isEqualTo("새 제목");
        assertThat(notice.getContent()).isEqualTo("새 내용");
        assertThat(notice.getUpdatedAt()).isEqualTo(updatedNow);
        assertThat(notice.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void updateRejectsNullNow() {
        Notice notice = Notice.create(7L, "제목", "내용", false, List.of(), NOW);

        assertThatThrownBy(() -> notice.update("새 제목", "새 내용", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void threeArgUpdateLeavesAttachmentsUntouchedAndMarksNotReplaced() {
        NoticeAttachment original = NoticeAttachment.create(1L, "원본.pdf");
        Notice notice = Notice.create(7L, "제목", "내용", false, List.of(original), NOW);

        notice.update("새 제목", "새 내용", NOW.plusHours(1));

        assertThat(notice.getAttachments()).containsExactly(original);
        assertThat(notice.isAttachmentsReplaced()).isFalse();
    }

    @Test
    void fourArgUpdateWithAttachmentsReplacesThemAndMarksReplaced() {
        Notice notice = Notice.create(7L, "제목", "내용", false,
                List.of(NoticeAttachment.create(1L, "원본.pdf")), NOW);

        NoticeAttachment replacement = NoticeAttachment.create(2L, "교체.pdf");
        notice.update("새 제목", "새 내용", List.of(replacement), NOW.plusHours(1));

        assertThat(notice.getAttachments()).containsExactly(replacement);
        assertThat(notice.isAttachmentsReplaced()).isTrue();
    }

    @Test
    void fourArgUpdateWithEmptyListClearsAttachments() {
        Notice notice = Notice.create(7L, "제목", "내용", false,
                List.of(NoticeAttachment.create(1L, "원본.pdf")), NOW);

        notice.update("제목", "내용", List.of(), NOW.plusHours(1));

        assertThat(notice.getAttachments()).isEmpty();
        assertThat(notice.isAttachmentsReplaced()).isTrue();
    }

    @Test
    void isAuthorMatchesOnlyAuthorUserId() {
        Notice notice = Notice.create(7L, "제목", "내용", false, List.of(), NOW);

        assertThat(notice.isAuthor(7L)).isTrue();
        assertThat(notice.isAuthor(999L)).isFalse();
    }

    @Test
    void recordViewIncrementsViewCount() {
        Notice notice = Notice.create(7L, "제목", "내용", false, List.of(), NOW);

        notice.recordView();
        notice.recordView();

        assertThat(notice.getViewCount()).isEqualTo(2L);
    }

    @Test
    void pinAndUnpinTogglePinnedFlag() {
        Notice notice = Notice.create(7L, "제목", "내용", false, List.of(), NOW);

        notice.pin();
        assertThat(notice.isPinned()).isTrue();

        notice.unpin();
        assertThat(notice.isPinned()).isFalse();
    }
}
