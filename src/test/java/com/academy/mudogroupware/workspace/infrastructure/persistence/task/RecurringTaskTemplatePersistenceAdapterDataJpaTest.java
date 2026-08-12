package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskSkipRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskTemplateRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.WorkspacePersistenceTestConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(WorkspacePersistenceTestConfig.class)
class RecurringTaskTemplatePersistenceAdapterDataJpaTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long CREATOR_ID = 10L;

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;
  @Autowired private RecurringTaskTemplateRepository recurringTaskTemplateRepository;
  @Autowired private RecurringTaskSkipRepository recurringTaskSkipRepository;

  @Test
  void savesNewTemplateAndReturnsAssignedId() {
    insertWorkspace(WORKSPACE_ID);

    RecurringTaskTemplate saved =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "주간 출결 현황 정리", RecurrenceType.WEEKLY,
                Map.of("daysOfWeek", List.of(1)), CREATOR_ID));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getTitle()).isEqualTo("주간 출결 현황 정리");
  }

  @Test
  void findByWorkspaceIdAndIdReturnsEmptyForOtherWorkspace() {
    insertWorkspace(WORKSPACE_ID);
    insertWorkspace(2L);
    RecurringTaskTemplate saved =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "수납 현황 확인", RecurrenceType.MONTHLY,
                Map.of("dayOfMonth", 1), CREATOR_ID));

    Optional<RecurringTaskTemplate> foundWithWrongWorkspace =
        recurringTaskTemplateRepository.findByWorkspaceIdAndId(2L, saved.getId());
    Optional<RecurringTaskTemplate> foundWithCorrectWorkspace =
        recurringTaskTemplateRepository.findByWorkspaceIdAndId(WORKSPACE_ID, saved.getId());

    assertThat(foundWithWrongWorkspace).isEmpty();
    assertThat(foundWithCorrectWorkspace).isPresent();
  }

  @Test
  void findByWorkspaceIdAndIdForUpdateReturnsEmptyForOtherWorkspace() {
    insertWorkspace(WORKSPACE_ID);
    insertWorkspace(2L);
    RecurringTaskTemplate saved =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "락 대상", RecurrenceType.MONTHLY,
                Map.of("dayOfMonth", 1), CREATOR_ID));

    Optional<RecurringTaskTemplate> foundWithWrongWorkspace =
        recurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate(2L, saved.getId());
    Optional<RecurringTaskTemplate> foundWithCorrectWorkspace =
        recurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate(WORKSPACE_ID, saved.getId());

    assertThat(foundWithWrongWorkspace).isEmpty();
    assertThat(foundWithCorrectWorkspace).isPresent();
    assertThat(foundWithCorrectWorkspace.get().getId()).isEqualTo(saved.getId());
  }

  @Test
  void updateReplacesTitleAndRecurrence() {
    insertWorkspace(WORKSPACE_ID);
    RecurringTaskTemplate saved =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "기존 제목", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID));

    RecurringTaskTemplate updated =
        recurringTaskTemplateRepository.save(
            saved.changeRecurrence("새 제목", RecurrenceType.MONTHLY, Map.of("dayOfMonth", 1)));

    assertThat(updated.getId()).isEqualTo(saved.getId());
    assertThat(updated.getTitle()).isEqualTo("새 제목");
    assertThat(updated.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
  }

  @Test
  void deleteRemovesTemplateAndCascadesSkipRecords() {
    insertWorkspace(WORKSPACE_ID);
    RecurringTaskTemplate saved =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "삭제 대상", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    LocalDateTime scheduledFor = LocalDateTime.of(2026, 8, 10, 0, 0);
    recurringTaskSkipRepository.saveIfAbsent(saved.getId(), scheduledFor);

    recurringTaskTemplateRepository.delete(saved.getId());

    assertThat(recurringTaskTemplateRepository.findByWorkspaceIdAndId(WORKSPACE_ID, saved.getId()))
        .isEmpty();
    assertThat(recurringTaskSkipRepository.exists(saved.getId(), scheduledFor)).isFalse();
  }

  @Test
  void deleteKeepsGeneratedTaskButNullsRecurringTemplateReference() {
    insertWorkspace(WORKSPACE_ID);
    RecurringTaskTemplate saved =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "삭제 대상", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    Long generatedTaskId = insertTaskReferencingTemplate(WORKSPACE_ID, saved.getId());

    recurringTaskTemplateRepository.delete(saved.getId());
    entityManager.flush();

    Long remainingTemplateRef =
        jdbcTemplate.queryForObject(
            "select recurring_template_id from task where task_id = ?", Long.class, generatedTaskId);
    assertThat(remainingTemplateRef).isNull();
  }

  private Long insertTaskReferencingTemplate(long workspaceId, long templateId) {
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(
        "insert into task (workspace_id, recurring_template_id, title, status, scheduled_for,"
            + " created_by, created_at, updated_at) values (?, ?, ?, 'WAITING', ?, ?, ?, ?)",
        workspaceId, templateId, "생성된 반복 업무", now, CREATOR_ID, now, now);
    return jdbcTemplate.queryForObject(
        "select task_id from task where recurring_template_id = ?", Long.class, templateId);
  }

  @Test
  void findAllReturnsTemplatesAcrossWorkspaces() {
    insertWorkspace(WORKSPACE_ID);
    insertWorkspace(2L);
    recurringTaskTemplateRepository.save(
        RecurringTaskTemplate.create(
            WORKSPACE_ID, "A", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    recurringTaskTemplateRepository.save(
        RecurringTaskTemplate.create(2L, "B", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID));

    assertThat(recurringTaskTemplateRepository.findAll()).hasSize(2);
  }

  @Test
  void findAllExcludesTemplatesInSoftDeletedWorkspace() {
    insertWorkspace(WORKSPACE_ID);
    insertWorkspace(2L);
    recurringTaskTemplateRepository.save(
        RecurringTaskTemplate.create(
            WORKSPACE_ID, "삭제된 워크스페이스 템플릿", RecurrenceType.WEEKLY,
            Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    recurringTaskTemplateRepository.save(
        RecurringTaskTemplate.create(
            2L, "정상 워크스페이스 템플릿", RecurrenceType.WEEKLY,
            Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    jdbcTemplate.update(
        "update workspace set deleted_at = ? where workspace_id = ?",
        LocalDateTime.now(), WORKSPACE_ID);

    assertThat(recurringTaskTemplateRepository.findAll())
        .extracting(RecurringTaskTemplate::getTitle)
        .containsExactly("정상 워크스페이스 템플릿");
  }

  @Test
  void findAllByWorkspaceIdReturnsNewestFirstWithinPageSize() {
    insertWorkspace(WORKSPACE_ID);
    insertWorkspace(2L);
    RecurringTaskTemplate oldest =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "가장 오래된 템플릿", RecurrenceType.WEEKLY,
                Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    RecurringTaskTemplate newest =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "가장 최근 템플릿", RecurrenceType.WEEKLY,
                Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    recurringTaskTemplateRepository.save(
        RecurringTaskTemplate.create(
            2L, "다른 워크스페이스", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID));

    PageResult<RecurringTaskTemplate> firstPage =
        recurringTaskTemplateRepository.findAllByWorkspaceId(WORKSPACE_ID, 0, 1);

    assertThat(firstPage.content()).extracting(RecurringTaskTemplate::getId).containsExactly(newest.getId());
    assertThat(firstPage.page()).isEqualTo(0);
    assertThat(firstPage.size()).isEqualTo(1);
    assertThat(firstPage.hasNext()).isTrue();

    PageResult<RecurringTaskTemplate> secondPage =
        recurringTaskTemplateRepository.findAllByWorkspaceId(WORKSPACE_ID, 1, 1);

    assertThat(secondPage.content()).extracting(RecurringTaskTemplate::getId).containsExactly(oldest.getId());
    assertThat(secondPage.hasNext()).isFalse();
  }

  @Test
  void findAllByWorkspaceIdBreaksCreatedAtTieByIdDescending() {
    insertWorkspace(WORKSPACE_ID);
    RecurringTaskTemplate lowerId =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "동시 생성 A", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    RecurringTaskTemplate higherId =
        recurringTaskTemplateRepository.save(
            RecurringTaskTemplate.create(
                WORKSPACE_ID, "동시 생성 B", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), CREATOR_ID));
    // 두 템플릿의 created_at을 강제로 동일하게 만들어, id가 없으면 정렬이 비결정적임을 검증한다.
    LocalDateTime sameInstant = LocalDateTime.of(2026, 8, 9, 0, 0);
    jdbcTemplate.update(
        "update recurring_task_template set created_at = ? where recurring_template_id in (?, ?)",
        sameInstant, lowerId.getId(), higherId.getId());

    PageResult<RecurringTaskTemplate> firstPage =
        recurringTaskTemplateRepository.findAllByWorkspaceId(WORKSPACE_ID, 0, 1);
    PageResult<RecurringTaskTemplate> secondPage =
        recurringTaskTemplateRepository.findAllByWorkspaceId(WORKSPACE_ID, 1, 1);

    assertThat(firstPage.content()).extracting(RecurringTaskTemplate::getId).containsExactly(higherId.getId());
    assertThat(secondPage.content()).extracting(RecurringTaskTemplate::getId).containsExactly(lowerId.getId());
  }

  private void insertWorkspace(long workspaceId) {
    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(
        "insert into workspace (workspace_id, name, created_by, created_at, updated_at) "
            + "values (?, ?, ?, ?, ?)",
        workspaceId,
        "워크스페이스" + workspaceId,
        CREATOR_ID,
        now,
        now);
  }
}
