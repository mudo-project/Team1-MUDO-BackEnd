package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recurring_task_skip")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringTaskSkipJpaEntity extends CreatedAtEntity {

  @EmbeddedId
  private RecurringTaskSkipId id;

  @MapsId("recurringTemplateId")
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recurring_template_id", nullable = false)
  private RecurringTaskTemplateJpaEntity recurringTemplate;

  private RecurringTaskSkipJpaEntity(
      RecurringTaskTemplateJpaEntity recurringTemplate, LocalDateTime scheduledFor) {
    this.id = RecurringTaskSkipId.of(recurringTemplate.getId(), scheduledFor);
    this.recurringTemplate = recurringTemplate;
  }

  public static RecurringTaskSkipJpaEntity create(
      RecurringTaskTemplateJpaEntity recurringTemplate, LocalDateTime scheduledFor) {
    return new RecurringTaskSkipJpaEntity(recurringTemplate, scheduledFor);
  }

  public LocalDateTime getScheduledFor() {
    return id.getScheduledFor();
  }
}
