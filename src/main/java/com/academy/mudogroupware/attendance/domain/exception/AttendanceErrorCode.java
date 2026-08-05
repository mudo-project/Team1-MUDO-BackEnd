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
            "와이파이 IP 메모는 100자 이하여야 합니다."),
    WIFI_IP_DELETION_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "ACADEMY_403_2",
            "와이파이 IP를 삭제할 권한이 없습니다."),
    WIFI_IP_VIEW_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "ACADEMY_403_3",
            "와이파이 IP를 조회할 권한이 없습니다."),
    WIFI_IP_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ACADEMY_404_1",
            "등록된 와이파이 IP를 찾을 수 없습니다."),
    ATTENDANCE_POLICY_SAVE_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "ATTENDANCE_403_1",
            "근무시간 정책을 저장할 권한이 없습니다."),
    INVALID_ATTENDANCE_POLICY(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_1",
            "유효하지 않은 근무시간 정책입니다."),
    INVALID_ATTENDANCE_POLICY_WEEKDAY(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_2",
            "유효하지 않은 요일별 근무 설정입니다."),
    DUPLICATE_ATTENDANCE_POLICY_WEEKDAY(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_3",
            "같은 요일을 중복해서 설정할 수 없습니다."),
    LATE_NOTE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_4",
            "지각한 경우 출근 사유를 입력해야 합니다."),
    INVALID_CLOCK_IN_NOTE(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_5",
            "출근 메모는 255자 이하여야 합니다."),
    INVALID_ATTENDANCE_RECORD(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_6",
            "유효하지 않은 출근 기록입니다."),
    INVALID_CLOCK_OUT_NOTE(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_7",
            "퇴근 메모는 255자 이하여야 합니다."),
    INVALID_CLOCK_OUT_TIME(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_8",
            "퇴근 시각은 출근 시각보다 빠를 수 없습니다."),
    OVERTIME_NOTE_REQUIRED(
            HttpStatus.BAD_REQUEST,
            "ATTENDANCE_400_9",
            "초과근무인 경우 퇴근 사유를 입력해야 합니다."),
    CHECK_IN_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "ATTENDANCE_403_2",
            "출근을 등록할 수 있는 학원 소속이 아닙니다."),
    UNREGISTERED_CHECK_IN_IP(
            HttpStatus.FORBIDDEN,
            "ATTENDANCE_403_3",
            "등록된 학원 IP에서만 출근할 수 있습니다."),
    CHECK_OUT_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "ATTENDANCE_403_4",
            "퇴근을 등록할 수 있는 학원 소속이 아닙니다."),
    TEAM_ATTENDANCE_VIEW_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "ATTENDANCE_403_6",
            "오늘 팀 근태 현황을 조회할 권한이 없습니다."),
    UNREGISTERED_CHECK_OUT_IP(
            HttpStatus.FORBIDDEN,
            "ATTENDANCE_403_5",
            "등록된 학원 IP에서만 퇴근할 수 있습니다."),
    ATTENDANCE_POLICY_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ATTENDANCE_404_1",
            "학원의 근무시간 정책을 찾을 수 없습니다."),
    ATTENDANCE_CHECK_IN_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "ATTENDANCE_404_2",
            "퇴근할 출근 기록을 찾을 수 없습니다."),
    ATTENDANCE_ALREADY_CHECKED_IN(
            HttpStatus.CONFLICT,
            "ATTENDANCE_409_1",
            "오늘은 이미 출근이 등록되었습니다."),
    ATTENDANCE_NON_WORKDAY(
            HttpStatus.CONFLICT,
            "ATTENDANCE_409_2",
            "오늘은 근무일이 아닙니다."),
    ATTENDANCE_ALREADY_CHECKED_OUT(
            HttpStatus.CONFLICT,
            "ATTENDANCE_409_3",
            "오늘은 이미 퇴근이 등록되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
