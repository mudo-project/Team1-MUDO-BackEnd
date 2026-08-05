package com.academy.mudogroupware.memo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.memo.domain.exception.MemoException;

class MemoTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 12, 0);

    @Test
    void createSucceedsWithValidInputs() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);

        assertThat(memo.getUserId()).isEqualTo(1L);
        assertThat(memo.getTitle()).isEqualTo("제목");
        assertThat(memo.getColor()).isEqualTo(MemoColor.YELLOW);
        assertThat(memo.getPositionX()).isNull();
        assertThat(memo.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void createRejectsNullUserId() {
        assertThatThrownBy(() -> Memo.create(null, "제목", "내용", MemoColor.YELLOW, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsBlankTitle() {
        assertThatThrownBy(() -> Memo.create(1L, " ", "내용", MemoColor.YELLOW, NOW))
                .isInstanceOf(MemoException.class);
    }

    @Test
    void createRejectsNullColor() {
        assertThatThrownBy(() -> Memo.create(1L, "제목", "내용", null, NOW))
                .isInstanceOf(MemoException.class);
    }

    @Test
    void createAcceptsTitleAtMaxLength() {
        String title = "가".repeat(100);

        Memo memo = Memo.create(1L, title, "내용", MemoColor.YELLOW, NOW);

        assertThat(memo.getTitle()).hasSize(100);
    }

    @Test
    void createRejectsTitleOverMaxLength() {
        String title = "가".repeat(101);

        assertThatThrownBy(() -> Memo.create(1L, title, "내용", MemoColor.YELLOW, NOW))
                .isInstanceOf(MemoException.class);
    }

    @Test
    void updateContentChangesTitleAndContentAndUpdatedAt() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);
        LocalDateTime later = NOW.plusHours(1);

        memo.updateContent("새 제목", "새 내용", later);

        assertThat(memo.getTitle()).isEqualTo("새 제목");
        assertThat(memo.getContent()).isEqualTo("새 내용");
        assertThat(memo.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void updateContentRejectsBlankTitle() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);

        assertThatThrownBy(() -> memo.updateContent(" ", "내용", NOW))
                .isInstanceOf(MemoException.class);
    }

    @Test
    void updateContentRejectsTitleOverMaxLength() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);
        String title = "가".repeat(101);

        assertThatThrownBy(() -> memo.updateContent(title, "내용", NOW))
                .isInstanceOf(MemoException.class);
    }

    @Test
    void updateColorChangesColorAndUpdatedAt() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);
        LocalDateTime later = NOW.plusHours(1);

        memo.updateColor(MemoColor.BLUE, later);

        assertThat(memo.getColor()).isEqualTo(MemoColor.BLUE);
        assertThat(memo.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void updateColorRejectsNullColor() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);

        assertThatThrownBy(() -> memo.updateColor(null, NOW))
                .isInstanceOf(MemoException.class);
    }

    @Test
    void updatePositionChangesPositionSizeAndUpdatedAt() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);
        LocalDateTime later = NOW.plusHours(1);

        memo.updatePosition(10, 20, 200, 150, later);

        assertThat(memo.getPositionX()).isEqualTo(10);
        assertThat(memo.getPositionY()).isEqualTo(20);
        assertThat(memo.getWidth()).isEqualTo(200);
        assertThat(memo.getHeight()).isEqualTo(150);
        assertThat(memo.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void updatePositionRejectsNonPositiveWidthOrHeight() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);

        assertThatThrownBy(() -> memo.updatePosition(0, 0, 0, 150, NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> memo.updatePosition(0, 0, 200, 0, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isOwnedByMatchesOwnerOnly() {
        Memo memo = Memo.create(1L, "제목", "내용", MemoColor.YELLOW, NOW);

        assertThat(memo.isOwnedBy(1L)).isTrue();
        assertThat(memo.isOwnedBy(2L)).isFalse();
    }
}
