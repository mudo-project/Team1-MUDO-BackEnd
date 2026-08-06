package com.academy.mudogroupware.approval.domain.event;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 결재 신청 시 휴가 기간(startDate~endDate)이 함께 입력된 경우에만 발행되는 이벤트.
 * attendance 모듈이 이 이벤트를 구독해 "승인 대기 중인 휴가"로 자체 저장한다.
 * approval은 이 기간을 ApprovalDocument에 영구 저장하지 않는다 — 신청 시점에 이 이벤트로
 * 흘려보내고 끝이며, 최종 승인/반려 여부는 {@link ApprovalDocumentDecidedEvent}로 별도 통지한다.
 */
public record LeaveRequestSubmittedEvent(Long documentId, Long academyId, Long requesterId,
                                          LocalDate startDate, LocalDate endDate, LocalDateTime submittedAt) {
}
