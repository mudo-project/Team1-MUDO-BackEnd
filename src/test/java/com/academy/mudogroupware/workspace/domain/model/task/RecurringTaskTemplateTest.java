package com.academy.mudogroupware.workspace.domain.model.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.workspace.domain.exception.task.InvalidRecurrenceRuleException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecurringTaskTemplateTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long CREATOR_ID = 10L;

  @Test
  void createsDailyTemplateWithEmptyRule() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.create(WORKSPACE_ID, "  일일 점검  ", RecurrenceType.DAILY, Map.of(), CREATOR_ID);

    assertThat(template.getTitle()).isEqualTo("일일 점검");
    assertThat(template.getRecurrenceType()).isEqualTo(RecurrenceType.DAILY);
  }

  @Test
  void rejectsDailyTemplateWithNonEmptyRule() {
    assertThatThrownBy(
            () ->
                RecurringTaskTemplate.create(
                    WORKSPACE_ID, "일일 점검", RecurrenceType.DAILY, Map.of("dayOfMonth", 1), CREATOR_ID))
        .isInstanceOf(InvalidRecurrenceRuleException.class);
  }

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
  void createsMonthlyTemplateWithDayOfMonth() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.create(
            WORKSPACE_ID, "수납 현황 확인", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 1), CREATOR_ID);

    assertThat(template.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
  }

  @Test
  void rejectsMonthlyTemplateWithOutOfRangeDay() {
    assertThatThrownBy(
            () ->
                RecurringTaskTemplate.create(
                    WORKSPACE_ID, "수납", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 32), CREATOR_ID))
        .isInstanceOf(InvalidRecurrenceRuleException.class);
  }

  @Test
  void changeRecurrenceReplacesTitleAndRule() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(
            1L, WORKSPACE_ID, "기존 제목", RecurrenceType.DAILY, Map.of(), CREATOR_ID);

    RecurringTaskTemplate changed =
        template.changeRecurrence(
            "새 제목", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 15));

    assertThat(changed.getId()).isEqualTo(1L);
    assertThat(changed.getTitle()).isEqualTo("새 제목");
    assertThat(changed.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
  }

  @Test
  void isDueOnDailyIsAlwaysTrue() {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(1L, WORKSPACE_ID, "매일", RecurrenceType.DAILY, Map.of(), CREATOR_ID);

    assertThat(template.isDueOn(LocalDate.of(2026, 8, 10))).isTrue();
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
