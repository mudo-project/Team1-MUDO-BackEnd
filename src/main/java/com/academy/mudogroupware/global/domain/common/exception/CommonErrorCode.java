package com.academy.mudogroupware.global.domain.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
  INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400_1", "입력값이 올바르지 않습니다."),
  INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "COMMON_400_2", "잘못된 요청입니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401_1", "인증이 필요합니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON_403_1", "접근 권한이 없습니다."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404_1", "요청한 리소스를 찾을 수 없습니다."),
  CONFLICT(HttpStatus.CONFLICT, "COMMON_409_1", "요청이 현재 상태와 충돌합니다."),
  TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "COMMON_429_1", "요청이 너무 많습니다."),
  SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "COMMON_503_1", "서비스가 일시적으로 혼잡합니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500_1", "서버 내부 오류가 발생했습니다.");
  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
