package com.academy.mudogroupware.workspace.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceResponseCode implements ResponseCode {

  WORKSPACE_CREATED("WORKSPACE_201_1", "워크스페이스 생성에 성공했습니다."),
  WORKSPACE_LIST_RETRIEVED("WORKSPACE_200_1", "워크스페이스 목록 조회에 성공했습니다.");

  private final String code;
  private final String message;
}
