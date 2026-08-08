package com.academy.mudogroupware.workspace.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceResponseCode implements ResponseCode {

  WORKSPACE_CREATED("WORKSPACE_201_1", "워크스페이스 생성에 성공했습니다."),
  WORKSPACE_LIST_RETRIEVED("WORKSPACE_200_1", "워크스페이스 목록 조회에 성공했습니다."),
  WORKSPACE_DETAIL_RETRIEVED("WORKSPACE_200_2", "워크스페이스 상세 조회에 성공했습니다."),
  WORKSPACE_RENAMED("WORKSPACE_200_3", "워크스페이스 이름 수정에 성공했습니다."),
  WORKSPACE_MEMBER_ADDED("WORKSPACE_200_4", "참여자 추가에 성공했습니다."),
  WORKSPACE_RECOVERED("WORKSPACE_200_5", "워크스페이스 복구에 성공했습니다."),
  TASK_CREATED("WORKSPACE_201_2", "업무 생성에 성공했습니다."),
  TASK_UPDATED("WORKSPACE_200_6", "업무 수정에 성공했습니다."),
  TASK_COMMENT_CREATED("WORKSPACE_201_3", "업무 댓글 생성에 성공했습니다."),
  TASK_COMMENT_UPDATED("WORKSPACE_200_7", "업무 댓글 수정에 성공했습니다."),
  TASK_COMMENT_COMPLETE_TOGGLED("WORKSPACE_200_8", "업무 댓글 완료 상태 변경에 성공했습니다."),
  RECURRING_TEMPLATE_LIST_RETRIEVED("WORKSPACE_200_9", "반복 업무 템플릿 목록 조회에 성공했습니다."),
  RECURRING_TEMPLATE_UPDATED("WORKSPACE_200_10", "반복 업무 템플릿 수정에 성공했습니다."),
  RECURRING_TEMPLATE_CREATED("WORKSPACE_201_4", "반복 업무 템플릿 생성에 성공했습니다.");

  private final String code;
  private final String message;
}
