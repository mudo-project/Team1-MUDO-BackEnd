package com.academy.mudogroupware.workspace.domain.model.task;

import com.academy.mudogroupware.workspace.domain.exception.task.InvalidRecurrenceRuleException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class RecurringTaskTemplate {

  private final Long id;
  private final Long workspaceId;
  private final String title;
  private final RecurrenceType recurrenceType;
  private final Map<String, Object> recurrenceRule;
  private final Long createdBy;

  private RecurringTaskTemplate(
      Long id,
      Long workspaceId,
      String title,
      RecurrenceType recurrenceType,
      Map<String, Object> recurrenceRule,
      Long createdBy) {
    validateRule(recurrenceType, recurrenceRule);
    this.id = id;
    this.workspaceId = workspaceId;
    this.title = title;
    this.recurrenceType = recurrenceType;
    this.recurrenceRule = recurrenceRule;
    this.createdBy = createdBy;
  }

  public static RecurringTaskTemplate create(
      Long workspaceId,
      String title,
      RecurrenceType recurrenceType,
      Map<String, Object> recurrenceRule,
      Long createdBy) {
    return new RecurringTaskTemplate(
        null, workspaceId, title.trim(), recurrenceType, recurrenceRule, createdBy);
  }

  public static RecurringTaskTemplate restore(
      Long id,
      Long workspaceId,
      String title,
      RecurrenceType recurrenceType,
      Map<String, Object> recurrenceRule,
      Long createdBy) {
    return new RecurringTaskTemplate(id, workspaceId, title, recurrenceType, recurrenceRule, createdBy);
  }

  public RecurringTaskTemplate changeRecurrence(
      String newTitle, RecurrenceType newRecurrenceType, Map<String, Object> newRecurrenceRule) {
    return new RecurringTaskTemplate(
        id, workspaceId, newTitle.trim(), newRecurrenceType, newRecurrenceRule, createdBy);
  }

  // 오늘이 이 템플릿의 발생일인지 판단한다. recurrenceRule 해석은 이 도메인 안에서만 이뤄진다.
  public boolean isDueOn(LocalDate today) {
    return switch (recurrenceType) {
      case DAILY -> true;
      case WEEKLY -> daysOfWeek().contains(today.getDayOfWeek().getValue());
      case MONTHLY -> dayOfMonth() == today.getDayOfMonth();
    };
  }

  private static void validateRule(RecurrenceType type, Map<String, Object> rule) {
    if (rule == null) {
      throw new InvalidRecurrenceRuleException();
    }
    switch (type) {
      case DAILY -> {
        if (!rule.isEmpty()) {
          throw new InvalidRecurrenceRuleException();
        }
      }
      case WEEKLY -> {
        Object raw = rule.get("daysOfWeek");
        if (!(raw instanceof List<?> days) || days.isEmpty()) {
          throw new InvalidRecurrenceRuleException();
        }
        for (Object day : days) {
          if (!(day instanceof Number number) || number.intValue() < 1 || number.intValue() > 7) {
            throw new InvalidRecurrenceRuleException();
          }
        }
      }
      case MONTHLY -> {
        Object raw = rule.get("dayOfMonth");
        if (!(raw instanceof Number number) || number.intValue() < 1 || number.intValue() > 31) {
          throw new InvalidRecurrenceRuleException();
        }
      }
    }
  }

  private List<Integer> daysOfWeek() {
    List<?> raw = (List<?>) recurrenceRule.get("daysOfWeek");
    return raw.stream().map(value -> ((Number) value).intValue()).toList();
  }

  private int dayOfMonth() {
    return ((Number) recurrenceRule.get("dayOfMonth")).intValue();
  }
}
