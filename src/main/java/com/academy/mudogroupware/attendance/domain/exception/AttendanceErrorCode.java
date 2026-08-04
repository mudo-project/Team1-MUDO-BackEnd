package com.academy.mudogroupware.attendance.domain.exception;

import org.springframework.http.HttpStatus;

import com.academy.mudogroupware.global.domain.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceErrorCode implements ErrorCode {

    WIFI_IP_REGISTRATION_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "ACADEMY_403_1",
            "와이파이 IP를 등록할 권한이 없습니다."),
    WIFI_IP_ALREADY_REGISTERED(
            HttpStatus.CONFLICT,
            "ACADEMY_409_1",
            "이미 등록된 IP입니다."),
    WIFI_IP_CHANGED(
            HttpStatus.CONFLICT,
            "ACADEMY_409_2",
            "접속 IP가 변경되었습니다. 다시 확인해주세요."),
    INVALID_WIFI_IP(
            HttpStatus.BAD_REQUEST,
            "ACADEMY_400_1",
            "유효하지 않은 IP 주소입니다."),
    INVALID_WIFI_IP_NOTE(
            HttpStatus.BAD_REQUEST,
            "ACADEMY_400_2",
            "와이파이 IP 메모는 100자 이하여야 합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
