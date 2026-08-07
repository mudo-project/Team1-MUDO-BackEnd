package com.academy.mudogroupware.attendance.presentation.api.common;

import com.academy.mudogroupware.global.presentation.api.common.ResponseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AttendanceResponseCode implements ResponseCode {

    CURRENT_CLIENT_IP_RETRIEVED("ACADEMY_200_1", "현재 접속 IP가 조회되었습니다."),
    ATTENDANCE_POLICY_SAVED("ATTENDANCE_200_1", "근무시간 정책이 저장되었습니다."),
    WIFI_IP_REGISTERED("ACADEMY_201_1", "와이파이 IP가 등록되었습니다."),
    WIFI_IP_DELETED("ACADEMY_200_2", "와이파이 IP가 삭제되었습니다."),
    WIFI_IP_LIST_RETRIEVED("ACADEMY_200_3", "등록된 와이파이 IP 목록을 조회했습니다."),
    ATTENDANCE_CHECKED_IN("ATTENDANCE_201_1", "출근이 등록되었습니다."),
    ATTENDANCE_CHECKED_OUT("ATTENDANCE_200_2", "퇴근이 등록되었습니다."),
    MY_ATTENDANCE_DAY_RETRIEVED("ATTENDANCE_200_9", "선택한 날짜의 근태가 조회되었습니다."),
    MY_CORRECTION_REQUESTS_RETRIEVED("ATTENDANCE_200_10", "내 근태 수정 요청 목록이 조회되었습니다."),
    MY_CORRECTION_REQUEST_RETRIEVED("ATTENDANCE_200_11", "내 근태 수정 요청이 조회되었습니다."),
    CORRECTION_REQUEST_CREATED("ATTENDANCE_201_2", "근태 수정 요청이 등록되었습니다."),
    TODAY_TEAM_ATTENDANCE_RETRIEVED(
            "ATTENDANCE_200_3", "오늘 팀 근태 현황을 조회했습니다.");

    private final String code;
    private final String message;
}
