package com.academy.mudogroupware.workspace.infrastructure.persistence.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(TimeConfig.class)
class TaskCommentPersistenceAdapterDataJpaTest {

  @Autowired private WorkspaceJpaRepository workspaceJpaRepository;
  @Autowired private TaskJpaRepository taskJpaRepository;
  @Autowired private TaskCommentJpaRepository taskCommentJpaRepository;
  @Autowired private TaskCommentMentionJpaRepository taskCommentMentionJpaRepository;

  private TaskCommentPersistenceAdapter adapter() {
    return new TaskCommentPersistenceAdapter(
        taskCommentJpaRepository,
        taskCommentMentionJpaRepository,
        taskJpaRepository,
        new TaskCommentPersistenceMapper() {});
  }

  private Long givenTaskId() {
    WorkspaceJpaEntity workspace =
        workspaceJpaRepository.save(WorkspaceJpaEntity.create(1L, "워크스페이스", 10L));
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
    Long taskId = givenTaskId();
    TaskComment older = adapter().save(
        TaskComment.create(taskId, 10L, "먼저 쓴 댓글", List.of(), LocalDateTime.of(2026, 8, 1, 16, 0)));
    TaskComment newer = adapter().save(
        TaskComment.create(taskId, 10L, "나중에 쓴 댓글", List.of(), LocalDateTime.of(2026, 8, 2, 18, 0)));

    PageResult<TaskComment> firstPage = adapter().findAllByTaskId(taskId, 0, 1);

    assertThat(firstPage.content()).extracting(TaskComment::getId).containsExactly(older.getId());
    assertThat(firstPage.hasNext()).isTrue();

    PageResult<TaskComment> secondPage = adapter().findAllByTaskId(taskId, 1, 1);
    assertThat(secondPage.content()).extracting(TaskComment::getId).containsExactly(newer.getId());
    assertThat(secondPage.hasNext()).isFalse();
  }

  @Test
  void findAllByTaskIdExcludesOtherTaskComments() {
    WorkspaceJpaEntity workspace =
        workspaceJpaRepository.save(WorkspaceJpaEntity.create(1L, "워크스페이스", 10L));
    Long taskId = givenTaskId(workspace);
    Long otherTaskId = givenTaskId(workspace);
    adapter().save(TaskComment.create(taskId, 10L, "이 업무 댓글", List.of(), LocalDateTime.of(2026, 8, 1, 9, 0)));
    adapter().save(TaskComment.create(otherTaskId, 10L, "다른 업무 댓글", List.of(), LocalDateTime.of(2026, 8, 1, 9, 0)));

    PageResult<TaskComment> page = adapter().findAllByTaskId(taskId, 0, 20);

    assertThat(page.content()).hasSize(1);
    assertThat(page.content().get(0).getContent()).isEqualTo("이 업무 댓글");
  }
}
