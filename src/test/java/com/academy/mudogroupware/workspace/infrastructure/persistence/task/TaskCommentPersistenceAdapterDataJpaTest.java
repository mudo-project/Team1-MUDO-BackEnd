package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.global.infrastructure.config.TimeConfig;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.workspace.WorkspaceJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
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
}
