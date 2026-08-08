package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "recurring_task_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurringTaskTemplateJpaEntity extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "recurring_template_id")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id", nullable = false)
  private WorkspaceJpaEntity workspace;

  @Column(nullable = false, length = 200)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(name = "recurrence_type", nullable = false, length = 20)
  private RecurrenceType recurrenceType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "recurrence_rule", nullable = false, columnDefinition = "json")
  private Map<String, Object> recurrenceRule;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  @Column(name = "created_by", nullable = false, updatable = false)
  private Long createdBy;

  private RecurringTaskTemplateJpaEntity(
      WorkspaceJpaEntity workspace,
      String title,
      RecurrenceType recurrenceType,
      Map<String, Object> recurrenceRule,
      Long createdBy) {
    this.workspace = workspace;
    this.title = title;
    this.recurrenceType = recurrenceType;
    this.recurrenceRule = recurrenceRule;
    this.active = true;
    this.createdBy = createdBy;
  }

  public static RecurringTaskTemplateJpaEntity create(
      WorkspaceJpaEntity workspace,
      String title,
      RecurrenceType recurrenceType,
      Map<String, Object> recurrenceRule,
      Long createdBy) {
    return new RecurringTaskTemplateJpaEntity(
        workspace, title, recurrenceType, recurrenceRule, createdBy);
  }
}
