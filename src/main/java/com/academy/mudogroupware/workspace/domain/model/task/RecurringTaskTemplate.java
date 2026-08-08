package com.academy.mudogroupware.workspace.domain.model.task;

import com.academy.mudogroupware.workspace.domain.exception.task.InvalidRecurrenceRuleException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
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
    this.recurrenceRule = copyOf(recurrenceRule);
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
      case WEEKLY -> daysOfWeek().contains(today.getDayOfWeek().getValue());
      case MONTHLY -> dayOfMonth() == today.getDayOfMonth();
    };
  }

  private static void validateRule(RecurrenceType type, Map<String, Object> rule) {
    if (rule == null) {
      throw new InvalidRecurrenceRuleException();
    }
    switch (type) {
      case WEEKLY -> {
        Object raw = rule.get("daysOfWeek");
        if (!(raw instanceof List<?> days) || days.isEmpty()) {
          throw new InvalidRecurrenceRuleException();
        }
        for (Object day : days) {
          if (!(day instanceof Number number) || !isWholeNumber(number)
              || number.intValue() < 1 || number.intValue() > 7) {
            throw new InvalidRecurrenceRuleException();
          }
        }
      }
      // 프론트 요청으로 매달 1일만 허용한다. 다른 날짜는 명시적으로 거부한다(2026-08-08).
      case MONTHLY -> {
        Object raw = rule.get("dayOfMonth");
        if (!(raw instanceof Number number) || !isWholeNumber(number) || number.intValue() != 1) {
          throw new InvalidRecurrenceRuleException();
        }
      }
    }
  }

  // 1.5처럼 소수부가 있는 값은 intValue()가 잘라버려 범위 검증을 우회할 수 있어 정수 여부를 먼저 확인한다.
  private static boolean isWholeNumber(Number number) {
    double value = number.doubleValue();
    return !Double.isInfinite(value) && !Double.isNaN(value) && value == Math.floor(value);
  }

  // 호출자가 넘긴 Map/List를 그대로 들고 있으면 생성 이후 외부에서 변형할 수 있어 깊은 불변 복사본을 만든다.
  private static Map<String, Object> copyOf(Map<String, Object> rule) {
    Map<String, Object> copy = new HashMap<>();
    rule.forEach((key, value) -> copy.put(key, value instanceof List<?> list ? List.copyOf(list) : value));
    return Collections.unmodifiableMap(copy);
  }

  private List<Integer> daysOfWeek() {
    List<?> raw = (List<?>) recurrenceRule.get("daysOfWeek");
    return raw.stream().map(value -> ((Number) value).intValue()).toList();
  }

  private int dayOfMonth() {
    return ((Number) recurrenceRule.get("dayOfMonth")).intValue();
  }
}
