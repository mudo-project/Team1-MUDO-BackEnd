package com.academy.mudogroupware.revenuereport.domain.event;

import java.time.LocalDate;

/**
 * 매출 리포트가 새로 생성되었을 때 발행되는 이벤트. notification 도메인이 구독해 알림함에 저장한다.
 */
public record RevenueReportGeneratedEvent(Long recipientUserId, Long reportId, LocalDate targetMonth) {
}
