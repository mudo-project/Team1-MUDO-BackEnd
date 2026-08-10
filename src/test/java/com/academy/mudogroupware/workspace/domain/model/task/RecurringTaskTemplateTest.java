package com.academy.mudogroupware.workspace.domain.model.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.workspace.domain.exception.task.InvalidRecurrenceRuleException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecurringTaskTemplateTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long CREATOR_ID = 10L;

  @Test
  void createsWeeklyTemplateWithDaysOfWeek() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.create(
            WORKSPACE_ID,
            "주간 출결 현황 정리",
            RecurrenceType.WEEKLY,
            Map.of("daysOfWeek", List.of(1, 3, 5)),
            CREATOR_ID);

    assertThat(template.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
  }

  @Test
  void rejectsWeeklyTemplateWithEmptyDaysOfWeek() {
    assertThatThrownBy(
            () ->
                RecurringTaskTemplate.create(
                    WORKSPACE_ID, "주간", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of()), CREATOR_ID))
        .isInstanceOf(InvalidRecurrenceRuleException.class);
  }

  @Test
  void rejectsWeeklyTemplateWithOutOfRangeDay() {
    assertThatThrownBy(
            () ->
                RecurringTaskTemplate.create(
                    WORKSPACE_ID, "주간", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(8)), CREATOR_ID))
        .isInstanceOf(InvalidRecurrenceRuleException.class);
  }

  @Test
  void rejectsWeeklyTemplateWithZeroDay() {
    assertThatThrownBy(
            () ->
                RecurringTaskTemplate.create(
                    WORKSPACE_ID, "주간", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(0)), CREATOR_ID))
        .isInstanceOf(InvalidRecurrenceRuleException.class);
  }

  @Test
  void createsWeeklyTemplateWithDayOfWeekUpperBoundSeven() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.create(
            WORKSPACE_ID, "주간", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(7)), CREATOR_ID);

    assertThat(template.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
  }

  @Test
  void rejectsWeeklyTemplateWithNonIntegerDay() {
    assertThatThrownBy(
            () ->
                RecurringTaskTemplate.create(
                    WORKSPACE_ID, "주간", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1.5)), CREATOR_ID))
        .isInstanceOf(InvalidRecurrenceRuleException.class);
  }

  @Test
  void createsMonthlyTemplateWithDayOfMonth() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.create(
            WORKSPACE_ID, "수납 현황 확인", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 1), CREATOR_ID);

    assertThat(template.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
  }

  @Test
  void rejectsMonthlyTemplateWithDayOfMonthOtherThanOne() {
    assertThatThrownBy(
            () ->
                RecurringTaskTemplate.create(
                    WORKSPACE_ID, "수납", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 2), CREATOR_ID))
        .isInstanceOf(InvalidRecurrenceRuleException.class);
  }

  @Test
  void rejectsMonthlyTemplateWithNonIntegerDay() {
    assertThatThrownBy(
            () ->
                RecurringTaskTemplate.create(
                    WORKSPACE_ID, "수납", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 1.5), CREATOR_ID))
        .isInstanceOf(InvalidRecurrenceRuleException.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void recurrenceRuleIsIsolatedFromCallerMutation() {
    List<Object> daysOfWeek = new ArrayList<>(List.of(1, 3));
    Map<String, Object> rule = new HashMap<>();
    rule.put("daysOfWeek", daysOfWeek);
    RecurringTaskTemplate template =
        RecurringTaskTemplate.create(WORKSPACE_ID, "주간", RecurrenceType.WEEKLY, rule, CREATOR_ID);

    // 생성 이후 호출자가 넘긴 원본 Map/List를 바꿔도 템플릿 내부 상태는 영향받지 않는다.
    rule.put("daysOfWeek", List.of(9));
    daysOfWeek.add(5);
    assertThat(template.getRecurrenceRule().get("daysOfWeek")).isEqualTo(List.of(1, 3));

    // 반환된 규칙 자체도 외부에서 변경할 수 없다.
    assertThatThrownBy(() -> template.getRecurrenceRule().put("daysOfWeek", List.of(9)))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(
            () -> ((List<Object>) template.getRecurrenceRule().get("daysOfWeek")).add(5))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void changeRecurrenceReplacesTitleAndRule() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(
            1L, WORKSPACE_ID, "기존 제목", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID);

    RecurringTaskTemplate changed =
        template.changeRecurrence(
            "새 제목", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 1));

    assertThat(changed.getId()).isEqualTo(1L);
    assertThat(changed.getTitle()).isEqualTo("새 제목");
    assertThat(changed.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
  }

  @Test
  void isDueOnWeeklyMatchesConfiguredDayOfWeek() {
    // 2026-08-10은 월요일(ISO 1)
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(
            1L, WORKSPACE_ID, "주간", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1, 3)), CREATOR_ID);

    assertThat(template.isDueOn(LocalDate.of(2026, 8, 10))).isTrue();
    assertThat(template.isDueOn(LocalDate.of(2026, 8, 11))).isFalse();
  }

  @Test
  void isDueOnMonthlyMatchesConfiguredDayOfMonth() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(
            1L, WORKSPACE_ID, "월간", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 1), CREATOR_ID);

    assertThat(template.isDueOn(LocalDate.of(2026, 8, 1))).isTrue();
    assertThat(template.isDueOn(LocalDate.of(2026, 8, 2))).isFalse();
  }
}
