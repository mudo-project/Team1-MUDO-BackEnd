package com.academy.mudogroupware.global.domain.auth;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "유효하지 않은 토큰입니다."),
  EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_2", "만료된 토큰입니다."),
  INVALID_TOKEN_SUBJECT(HttpStatus.UNAUTHORIZED, "AUTH_401_3", "토큰의 사용자 식별자가 올바르지 않습니다."),
  USERNAME_CLAIM_MISSING(HttpStatus.UNAUTHORIZED, "AUTH_401_4", "토큰에 사용자명이 없습니다."),
  REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH_401_6", "저장된 리프레시 토큰이 없습니다."),
  REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH_401_7", "리프레시 토큰 정보가 일치하지 않습니다.");
  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
}
