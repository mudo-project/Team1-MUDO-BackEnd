package com.academy.mudogroupware.global.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeleteTimeEntity extends BaseTimeEntity {

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  public void markDeleted(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
