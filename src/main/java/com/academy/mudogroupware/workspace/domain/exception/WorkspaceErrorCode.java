package com.academy.mudogroupware.workspace.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {
  INVALID_MEMBER(
      HttpStatus.BAD_REQUEST, "WORKSPACE_400_1", "선택할 수 없는 참여자가 포함되어 있습니다."),
  NAME_CONFLICT(HttpStatus.CONFLICT, "WORKSPACE_409_1", "워크스페이스 이름을 생성할 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
