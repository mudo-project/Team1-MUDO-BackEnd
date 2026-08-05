package com.academy.mudogroupware.lecture.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class StudentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 0);

    @Test
    void createsStudentWithOptionalFieldsNull() {
        Student student = Student.create(1L, "이준호", Grade.MIDDLE_3, null, null, null, null, NOW);

        assertThat(student.getName()).isEqualTo("이준호");
        assertThat(student.getParentPhone()).isNull();
    }

    @Test
    void throwsWhenNameIsBlank() {
        assertThatThrownBy(() -> Student.create(1L, " ", Grade.MIDDLE_3, null, null, null, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
