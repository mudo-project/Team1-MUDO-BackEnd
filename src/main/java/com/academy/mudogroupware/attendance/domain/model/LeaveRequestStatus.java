package com.academy.mudogroupware.attendance.domain.model;

/**
 * approval 모듈의 결재 문서와 연동된 휴가 신청의 처리 상태.
 * PENDING: 결재 신청됨(아직 최종 승인/반려 전). CONFIRMED: 최종 승인됨(팀 근태 조회에 LEAVE로 반영).
 * CANCELLED: 반려됨(팀 근태 조회에 반영하지 않음, 이력 보존을 위해 삭제하지 않음).
 */
public enum LeaveRequestStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
