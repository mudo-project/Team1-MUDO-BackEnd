package com.academy.mudogroupware.global.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeleteTimeEntity extends BaseTimeEntity {

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void markDeleted(LocalDateTime deletedAt) {
    Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    if (this.deletedAt != null) {
      throw new IllegalStateException("이미 삭제된 엔티티입니다.");
    }
    this.deletedAt = deletedAt;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }

  public void clearDeletedAt() {
    if (this.deletedAt == null) {
      throw new IllegalStateException("삭제되지 않은 엔티티입니다.");
    }
    this.deletedAt = null;
  }
}
