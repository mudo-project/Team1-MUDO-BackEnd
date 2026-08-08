package com.academy.mudogroupware.workspace.presentation.api.response.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceDetail;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.application.query.task.WorkspaceTaskItem;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkspaceDetailResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void omitsCommentCountFieldsWhenTaskHasNoComments() throws Exception {
    WorkspaceMemberInfo creator = new WorkspaceMemberInfo(25L, "정다은");
    WorkspaceTaskItem withoutComments =
        new WorkspaceTaskItem(102L, "청소 당번", TaskStatus.WAITING, creator, null, null, null);
    WorkspaceDetail detail =
        new WorkspaceDetail(1L, "1월 학사 운영", List.of(), List.of(withoutComments));

    String json = objectMapper.writeValueAsString(WorkspaceDetailResponse.from(detail));

    assertThat(json).doesNotContain("completedCommentCount", "commentCount");
  }

  @Test
  void includesCommentCountFieldsWhenTaskHasComments() throws Exception {
    WorkspaceMemberInfo creator = new WorkspaceMemberInfo(25L, "정다은");
    WorkspaceTaskItem withComments =
        new WorkspaceTaskItem(
            101L, "청구서 발송", TaskStatus.IN_PROGRESS, creator, LocalDate.of(2026, 8, 7), 1L, 2L);
    WorkspaceDetail detail = new WorkspaceDetail(1L, "1월 학사 운영", List.of(), List.of(withComments));

    String json = objectMapper.writeValueAsString(WorkspaceDetailResponse.from(detail));

    assertThat(json).contains("\"completedCommentCount\":1", "\"commentCount\":2");
  }
}
