package com.academy.mudogroupware.attendance.infrastructure.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.academy.mudogroupware.approval.domain.event.ApprovalDocumentDecidedEvent;
import com.academy.mudogroupware.attendance.application.usecase.DecideLeaveRequestUseCase;

import lombok.RequiredArgsConstructor;

/**
 * approval 모듈이 발행하는 최종 승인·반려 이벤트를 소비해 attendance의 leave_request 상태에 반영한다.
 * approval의 Entity/Repository/Service를 직접 참조하지 않고, approval이 공개한 도메인 Event만 구독한다
 * (docs/MODULES.md "대상 모듈의 공개 Event 소비" 허용 범위).
 */
@Component
@RequiredArgsConstructor
public class ApprovalLeaveEventListener {

    private final DecideLeaveRequestUseCase decideLeaveRequestUseCase;

    // 리스너 메서드에는 @Transactional을 붙이지 않고, 위임받은 Application Service가 트랜잭션을 연다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApprovalDocumentDecided(ApprovalDocumentDecidedEvent event) {
        decideLeaveRequestUseCase.decide(event.documentId(), event.approved(), event.decidedAt());
    }
}
