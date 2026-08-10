package com.academy.mudogroupware.approval.domain.event;

import java.time.LocalDateTime;

/**
 * 결재 문서의 다음 결재선이 활성화(내 차례가 됨)되었을 때 발행되는 이벤트.
 * 현재 이 이벤트는 ApprovalWebSocketNotifier가 AFTER_COMMIT 시점에 소비해 STOMP로 전송한다.
 * Web Push 발송 리스너는 추가하지 않는다.
 */
public record ApprovalLineActivatedEvent(Long documentId, String documentTitle, Long approverId,
                                          LocalDateTime activatedAt) {
}
