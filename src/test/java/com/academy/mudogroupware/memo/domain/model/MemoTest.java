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
}
