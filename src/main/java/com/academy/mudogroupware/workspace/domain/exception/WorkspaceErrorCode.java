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
  LAST_MEMBER_CANNOT_LEAVE(
      HttpStatus.BAD_REQUEST, "WORKSPACE_400_2", "마지막 참여자는 나갈 수 없습니다."),
  NAME_CONFLICT(HttpStatus.CONFLICT, "WORKSPACE_409_1", "워크스페이스 이름을 생성할 수 없습니다."),
  ALREADY_ACTIVE(HttpStatus.CONFLICT, "WORKSPACE_409_2", "이미 활성 상태인 워크스페이스입니다."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "WORKSPACE_404_1", "워크스페이스를 찾을 수 없습니다."),
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKSPACE_404_2", "참여자를 찾을 수 없습니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "WORKSPACE_403_1", "워크스페이스에 접근할 권한이 없습니다."),
  INVALID_TASK_STATUS_TRANSITION(
      HttpStatus.BAD_REQUEST, "WORKSPACE_400_3", "허용되지 않은 업무 상태 변경입니다."),
  TASK_DUE_AT_REQUIRED(
      HttpStatus.BAD_REQUEST, "WORKSPACE_400_4", "기한이 지난 업무는 오늘 이후의 새 마감일과 함께 변경해야 합니다."),
  RECURRING_TASK_DUE_AT_NOT_ALLOWED(
      HttpStatus.BAD_REQUEST, "WORKSPACE_400_5", "반복 업무는 마감일을 수정할 수 없습니다."),
  TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "WORKSPACE_404_3", "업무를 찾을 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
