package com.academy.mudogroupware.attendance.infrastructure.event;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.approval.domain.event.ApprovalDocumentDecidedEvent;
import com.academy.mudogroupware.approval.domain.event.LeaveRequestSubmittedEvent;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;

/**
 * approval 모듈이 발행하는 휴가 연동 이벤트를 소비해 attendance 자체 leave_request 테이블에 반영한다.
 * approval의 Entity/Repository/Service를 직접 참조하지 않고, approval이 공개한 도메인 Event만 구독한다
 * (docs/MODULES.md "대상 모듈의 공개 Event 소비" 허용 범위).
 *
 * 이 파일 전체(leave_request 관련 domain/infrastructure 코드)는 attendance 담당자 리뷰 전
 * 제안 상태입니다 - 데이터 흐름은 approval 쪽 PR 설명 참고.
 */
@Component
@RequiredArgsConstructor
public class ApprovalLeaveEventListener {

    private final LeaveRequestRepository leaveRequestRepository;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeaveRequestSubmitted(LeaveRequestSubmittedEvent event) {
        LeaveRequest leaveRequest = LeaveRequest.submit(event.academyId(), event.requesterId(), event.documentId(),
                event.startDate(), event.endDate(), event.submittedAt());
        leaveRequestRepository.save(leaveRequest);
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApprovalDocumentDecided(ApprovalDocumentDecidedEvent event) {
        leaveRequestRepository.findByDocumentId(event.documentId()).ifPresent(leaveRequest -> {
            if (event.approved()) {
                leaveRequest.confirm(event.decidedAt());
            } else {
                leaveRequest.cancel(event.decidedAt());
            }
            leaveRequestRepository.save(leaveRequest);
        });
    }
}
