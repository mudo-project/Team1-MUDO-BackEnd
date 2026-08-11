package com.academy.mudogroupware.users.domain.exception;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USER_401_1", "아이디 또는 비밀번호가 올바르지 않습니다."),
    LOGIN_RESTRICTED(HttpStatus.FORBIDDEN, "USER_403_1", "로그인할 수 없는 계정 상태입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_401_2", "리프레시 토큰이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_1", "사용자를 찾을 수 없습니다."),
    ROLE_NAME_DUPLICATE(HttpStatus.CONFLICT, "USER_409_1", "이미 존재하는 역할 이름입니다."),
    ROLE_IN_USE(HttpStatus.CONFLICT, "USER_409_2", "이 역할을 사용 중인 구성원이 있어 삭제할 수 없습니다."),
    INVALID_PERMISSION_CODE(HttpStatus.BAD_REQUEST, "USER_400_1", "존재하지 않는 권한 코드입니다."),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404_2", "역할을 찾을 수 없습니다."),
    USERNAME_DUPLICATE(HttpStatus.CONFLICT, "USER_409_6", "이미 사용 중인 아이디입니다."),
    PASSWORD_SETUP_FAILED(HttpStatus.BAD_REQUEST, "USER_400_2", "비밀번호 설정에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
