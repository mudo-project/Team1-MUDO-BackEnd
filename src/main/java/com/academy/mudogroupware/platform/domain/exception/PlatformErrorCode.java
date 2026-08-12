package com.academy.mudogroupware.platform.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlatformErrorCode implements ErrorCode {
  ACADEMY_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "PLATFORM_400_1", "학원 코드가 필요합니다."),
  ACADEMY_NOT_FOUND(HttpStatus.NOT_FOUND, "PLATFORM_404_1", "조회할 학원을 찾을 수 없습니다."),
  METRICS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "PLATFORM_503_1", "운영 지표를 현재 조회할 수 없습니다.");

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
