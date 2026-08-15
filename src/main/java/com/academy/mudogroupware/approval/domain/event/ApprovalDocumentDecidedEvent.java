package com.academy.mudogroupware.approval.domain.event;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;

/**
 * 결재 문서가 최종 승인(APPROVED) 또는 반려(REJECTED)로 확정됐을 때 발행되는 이벤트.
 * 라인 단위 진행 상황을 알리는 {@link ApprovalLineActivatedEvent}와 달리, 문서 전체의
 * 최종 결과 1회만 알린다(중간 결재 단계 통과 시에는 발행되지 않는다). 신청 취소(CANCELLED)도
 * 후속 도메인과 알림 저장을 위해 이 이벤트로 전달한다.
 */
public record ApprovalDocumentDecidedEvent(Long documentId, Long requesterId, ApprovalStatus status,
                                            LocalDateTime decidedAt) {

    public ApprovalDocumentDecidedEvent {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (status == ApprovalStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("status must be decided");
        }
    }

    public boolean approved() {
        return status == ApprovalStatus.APPROVED;
    }
}
