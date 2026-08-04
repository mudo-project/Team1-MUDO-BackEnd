package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringTaskSkipId implements Serializable {

  @Column(name = "recurring_template_id", nullable = false)
  private Long recurringTemplateId;

  @Column(name = "scheduled_for", nullable = false)
  private LocalDateTime scheduledFor;

  private RecurringTaskSkipId(Long recurringTemplateId, LocalDateTime scheduledFor) {
    this.recurringTemplateId = recurringTemplateId;
    this.scheduledFor = scheduledFor;
  }

  public static RecurringTaskSkipId of(Long recurringTemplateId, LocalDateTime scheduledFor) {
    return new RecurringTaskSkipId(recurringTemplateId, scheduledFor);
  }
}
