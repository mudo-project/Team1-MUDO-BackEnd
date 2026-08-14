package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.WorkspacePersistenceTestConfig;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(WorkspacePersistenceTestConfig.class)
class TaskCommentPersistenceAdapterDataJpaTest {

  @Autowired private WorkspaceJpaRepository workspaceJpaRepository;
  @Autowired private TaskJpaRepository taskJpaRepository;
  @Autowired private TaskCommentJpaRepository taskCommentJpaRepository;
  @Autowired private TaskCommentMentionJpaRepository taskCommentMentionJpaRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  private TaskCommentPersistenceAdapter adapter() {
    return new TaskCommentPersistenceAdapter(
        taskCommentJpaRepository,
        taskCommentMentionJpaRepository,
        taskJpaRepository,
        new TaskCommentPersistenceMapper() {});
  }

  private Long givenTaskId() {
    WorkspaceJpaEntity workspace =
        workspaceJpaRepository.save(WorkspaceJpaEntity.create("워크스페이스", 10L));
    return givenTaskId(workspace);
  }

  private Long givenTaskId(WorkspaceJpaEntity workspace) {
    TaskJpaEntity task =
        taskJpaRepository.save(
            TaskJpaEntity.create(
                workspace, null, "업무", 10L, TaskStatus.WAITING, LocalDate.of(2026, 8, 10), null));
    return task.getId();
  }

  @Test
  void savesCommentWithMentionsAndReloadsThem() {
    Long taskId = givenTaskId();
    TaskComment comment =
        TaskComment.create(taskId, 10L, "댓글 내용", List.of(20L, 21L), LocalDateTime.of(2026, 8, 7, 9, 0));

    TaskComment saved = adapter().save(comment);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getMentions()).extracting(m -> m.getMentionedUserId())
        .containsExactlyInAnyOrder(20L, 21L);

    Optional<TaskComment> reloaded = adapter().findById(saved.getId());
    assertThat(reloaded).isPresent();
    assertThat(reloaded.get().getMentions()).hasSize(2);
  }

  @Test
  void updatingReplacesMentionsEntirely() {
    Long taskId = givenTaskId();
    TaskComment created = adapter().save(
        TaskComment.create(taskId, 10L, "원본", List.of(20L), LocalDateTime.of(2026, 8, 7, 9, 0)));

    TaskComment updated = created.updateContent("수정본", List.of(30L, 31L), LocalDateTime.of(2026, 8, 7, 10, 0));
    TaskComment saved = adapter().save(updated);

    assertThat(saved.getContent()).isEqualTo("수정본");
    assertThat(saved.getMentions()).extracting(m -> m.getMentionedUserId())
        .containsExactlyInAnyOrder(30L, 31L);
    assertThat(taskCommentMentionJpaRepository.findAllByCommentId(saved.getId())).hasSize(2);
  }

  @Test
  void updatingWithPartiallyOverlappingMentionsKeepsOverlappingRowIdentity() {
    Long taskId = givenTaskId();
    TaskComment created = adapter().save(
        TaskComment.create(taskId, 10L, "원본", List.of(20L, 21L), LocalDateTime.of(2026, 8, 7, 9, 0)));
    Long keptMentionId = taskCommentMentionJpaRepository.findAllByCommentId(created.getId()).stream()
        .filter(m -> m.getMentionedUserId().equals(20L))
        .findFirst()
        .orElseThrow()
        .getId();

    // 20L은 유지, 21L은 제거, 22L은 추가
    TaskComment updated = created.updateContent("수정본", List.of(20L, 22L), LocalDateTime.of(2026, 8, 7, 10, 0));
    TaskComment saved = adapter().save(updated);
    entityManager.flush();
    entityManager.clear();

    assertThat(saved.getMentions()).extracting(m -> m.getMentionedUserId())
        .containsExactlyInAnyOrder(20L, 22L);
    Long survivingMentionId = taskCommentMentionJpaRepository.findAllByCommentId(saved.getId()).stream()
        .filter(m -> m.getMentionedUserId().equals(20L))
        .findFirst()
        .orElseThrow()
        .getId();
    // 겹치는 멘션(20L)은 delete/insert 되지 않아야 하므로 PK가 그대로여야 한다.
    assertThat(survivingMentionId).isEqualTo(keptMentionId);
  }

  @Test
  void updatingWithIdenticalMentionsDoesNotThrowDuplicateKeyException() {
    Long taskId = givenTaskId();
    TaskComment created = adapter().save(
        TaskComment.create(taskId, 10L, "원본", List.of(20L, 21L), LocalDateTime.of(2026, 8, 7, 9, 0)));

    // 프론트가 멘션을 안 바꾸고 동일한 mentionedUserIds로 내용 수정만 요청하는 경우를 재현한다.
    TaskComment updated = created.updateContent("수정본", List.of(20L, 21L), LocalDateTime.of(2026, 8, 7, 10, 0));
    TaskComment saved = adapter().save(updated);

    assertThat(saved.getContent()).isEqualTo("수정본");
    assertThat(saved.getMentions()).extracting(m -> m.getMentionedUserId())
        .containsExactlyInAnyOrder(20L, 21L);
  }

  @Test
  void deletingCommentCascadesMentions() {
    Long taskId = givenTaskId();
    TaskComment created = adapter().save(
        TaskComment.create(taskId, 10L, "삭제될 댓글", List.of(20L), LocalDateTime.of(2026, 8, 7, 9, 0)));

    adapter().deleteById(created.getId());

    assertThat(taskCommentJpaRepository.findById(created.getId())).isEmpty();
    assertThat(taskCommentMentionJpaRepository.findAllByCommentId(created.getId())).isEmpty();
  }

  @Test
  void findAllByTaskIdReturnsOldestFirstWithinPageSize() {
    // TaskCommentJpaEntity.create()는 createdAt을 받지 않고 @CreatedDate가 실제 저장 시각을 찍으므로,
    // "먼저 저장"과 "의도한 createdAt"이 실제로 어긋나도 정렬이 createdAt을 기준으로 동작하는지
    // 검증하기 위해 insertion 순서를 의도한 시간 순서와 반대로 만들고, 저장 후 created_at을
    // jdbcTemplate으로 직접 덮어써서 id 순서와 createdAt 순서가 어긋나게 만든다.
    Long taskId = givenTaskId();
    TaskComment newer = adapter().save(
        TaskComment.create(taskId, 10L, "나중에 쓴 댓글", List.of(), LocalDateTime.of(2026, 8, 2, 18, 0)));
    TaskComment older = adapter().save(
        TaskComment.create(taskId, 10L, "먼저 쓴 댓글", List.of(), LocalDateTime.of(2026, 8, 1, 16, 0)));
    overwriteCreatedAt(older.getId(), LocalDateTime.of(2026, 8, 1, 16, 0));
    overwriteCreatedAt(newer.getId(), LocalDateTime.of(2026, 8, 2, 18, 0));
    entityManager.clear();

    PageResult<TaskComment> firstPage = adapter().findAllByTaskId(taskId, 0, 1);

    assertThat(firstPage.content()).extracting(TaskComment::getId).containsExactly(older.getId());
    assertThat(firstPage.hasNext()).isTrue();

    PageResult<TaskComment> secondPage = adapter().findAllByTaskId(taskId, 1, 1);
    assertThat(secondPage.content()).extracting(TaskComment::getId).containsExactly(newer.getId());
    assertThat(secondPage.hasNext()).isFalse();
  }

  private void overwriteCreatedAt(Long commentId, LocalDateTime createdAt) {
    jdbcTemplate.update(
        "update task_comment set created_at = ? where comment_id = ?", createdAt, commentId);
  }

  @Test
  void findAllByTaskIdExcludesOtherTaskComments() {
    WorkspaceJpaEntity workspace =
        workspaceJpaRepository.save(WorkspaceJpaEntity.create("워크스페이스", 10L));
    Long taskId = givenTaskId(workspace);
    Long otherTaskId = givenTaskId(workspace);
    adapter().save(TaskComment.create(taskId, 10L, "이 업무 댓글", List.of(), LocalDateTime.of(2026, 8, 1, 9, 0)));
    adapter().save(TaskComment.create(otherTaskId, 10L, "다른 업무 댓글", List.of(), LocalDateTime.of(2026, 8, 1, 9, 0)));

    PageResult<TaskComment> page = adapter().findAllByTaskId(taskId, 0, 20);

    assertThat(page.content()).hasSize(1);
    assertThat(page.content().get(0).getContent()).isEqualTo("이 업무 댓글");
  }

  @Test
  void updateCompletionDoesNotTouchMentions() {
    Long taskId = givenTaskId();
    TaskComment created = adapter().save(
        TaskComment.create(taskId, 10L, "댓글", List.of(20L, 21L), LocalDateTime.of(2026, 8, 7, 9, 0)));
    List<Long> mentionIdsBeforeToggle = mentionIdsOf(created.getId());

    TaskComment toggled = created.toggleComplete(10L, LocalDateTime.of(2026, 8, 7, 10, 0));
    TaskComment saved = adapter().updateCompletion(toggled);
    entityManager.flush();
    entityManager.clear();

    assertThat(saved.isCompleted()).isTrue();
    assertThat(saved.getCompletedBy()).isEqualTo(10L);
    assertThat(saved.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 8, 7, 10, 0));
    assertThat(saved.getMentions()).extracting(m -> m.getMentionedUserId())
        .containsExactlyInAnyOrder(20L, 21L);
    // 값·개수뿐 아니라 멘션 행의 PK 자체가 그대로인지 확인한다 — delete-then-recreate로 회귀하면
    // 값은 같아도 IDENTITY로 새 PK가 발급되므로 이 비교가 깨진다.
    assertThat(mentionIdsOf(saved.getId())).isEqualTo(mentionIdsBeforeToggle);
  }

  @Test
  void togglingCompletionRepeatedlyDoesNotThrowDuplicateMentionKeyException() {
    Long taskId = givenTaskId();
    TaskComment created = adapter().save(
        TaskComment.create(taskId, 10L, "댓글", List.of(20L), LocalDateTime.of(2026, 8, 7, 9, 0)));

    TaskComment completed = adapter().updateCompletion(
        created.toggleComplete(10L, LocalDateTime.of(2026, 8, 7, 10, 0)));
    TaskComment reopened = adapter().updateCompletion(
        completed.toggleComplete(10L, LocalDateTime.of(2026, 8, 7, 11, 0)));

    assertThat(completed.isCompleted()).isTrue();
    assertThat(completed.getCompletedAt()).isEqualTo(LocalDateTime.of(2026, 8, 7, 10, 0));
    assertThat(reopened.isCompleted()).isFalse();
    assertThat(reopened.getCompletedBy()).isNull();
    assertThat(reopened.getCompletedAt()).isNull();
    assertThat(taskCommentMentionJpaRepository.findAllByCommentId(created.getId()))
        .extracting(m -> m.getMentionedUserId())
        .containsExactly(20L);
  }

  private List<Long> mentionIdsOf(Long commentId) {
    return taskCommentMentionJpaRepository.findAllByCommentId(commentId).stream()
        .map(TaskCommentMentionJpaEntity::getId)
        .sorted()
        .toList();
  }
}
