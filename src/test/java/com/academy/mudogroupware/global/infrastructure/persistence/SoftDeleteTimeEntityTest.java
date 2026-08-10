package com.academy.mudogroupware.global.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class SoftDeleteTimeEntityTest {

    @Test
    void markDeletedRecordsDeletedAt() {
        TestEntity entity = new TestEntity();
        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 10, 10, 0);

        entity.markDeleted(deletedAt);

        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(entity.isDeleted()).isTrue();
    }

    @Test
    void markDeletedRejectsNullTime() {
        TestEntity entity = new TestEntity();

        assertThatThrownBy(() -> entity.markDeleted(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("deletedAt must not be null");
    }

    @Test
    void markDeletedRejectsDuplicateDeletionAndPreservesOriginalTime() {
        TestEntity entity = new TestEntity();
        LocalDateTime firstDeletedAt = LocalDateTime.of(2026, 8, 10, 10, 0);
        LocalDateTime secondDeletedAt = LocalDateTime.of(2026, 8, 10, 11, 0);

        entity.markDeleted(firstDeletedAt);

        assertThatThrownBy(() -> entity.markDeleted(secondDeletedAt))
                .isInstanceOf(IllegalStateException.class);
        assertThat(entity.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    void clearDeletedAtRestoresActiveState() {
        TestEntity entity = new TestEntity();
        entity.markDeleted(LocalDateTime.of(2026, 8, 10, 10, 0));

        entity.clearDeletedAt();

        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.isDeleted()).isFalse();
    }

    @Test
    void clearDeletedAtRejectsActiveEntity() {
        TestEntity entity = new TestEntity();

        assertThatThrownBy(entity::clearDeletedAt)
                .isInstanceOf(IllegalStateException.class);
    }

    private static class TestEntity extends SoftDeleteTimeEntity {
    }
}
