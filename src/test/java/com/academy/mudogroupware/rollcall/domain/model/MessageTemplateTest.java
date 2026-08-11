package com.academy.mudogroupware.rollcall.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class MessageTemplateTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    @Test
    void createsTemplateWithValidData() {
        MessageTemplate template = MessageTemplate.create( "결석 안내", AttendanceStatus.ABSENT,
                "오늘 {학생명} 학생이 결석하였습니다.", 99L, NOW);

        assertThat(template.getName()).isEqualTo("결석 안내");
        assertThat(template.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
    }

    @Test
    void throwsWhenContentIsBlank() {
        assertThatThrownBy(() -> MessageTemplate.create( "결석 안내", AttendanceStatus.ABSENT, " ", 99L, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateChangesNameAndContentButKeepsStatus() {
        MessageTemplate template = MessageTemplate.create( "결석 안내", AttendanceStatus.ABSENT,
                "오늘 {학생명} 학생이 결석하였습니다.", 99L, NOW);
        LocalDateTime later = NOW.plusHours(1);

        template.update("결석 안내(수정)", "새 내용", later);

        assertThat(template.getName()).isEqualTo("결석 안내(수정)");
        assertThat(template.getContent()).isEqualTo("새 내용");
        assertThat(template.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(template.getUpdatedAt()).isEqualTo(later);
    }
}
