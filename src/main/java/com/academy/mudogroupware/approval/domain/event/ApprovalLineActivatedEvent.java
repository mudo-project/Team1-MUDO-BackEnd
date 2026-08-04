package com.academy.mudogroupware.approval.domain.event;

import java.time.LocalDateTime;

/**
 * 결재 문서의 다음 결재선이 활성화(내 차례가 됨)되었을 때 발행되는 이벤트.
 * 실제 Web Push 발송 로직(구독 대상 조회 + 발송)은 아직 없다 — 프론트 서비스워커/VAPID 준비 후
 * 이 이벤트를 구독하는 리스너를 추가해서 연동한다.
 */
public record ApprovalLineActivatedEvent(Long documentId, String documentTitle, Long approverId,
                                          LocalDateTime activatedAt) {
}
