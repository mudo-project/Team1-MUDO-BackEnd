package com.academy.mudogroupware.approval.application.port;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface LeaveRequestSubmissionPort {

    // academyId: attendance 모듈은 아직 학원별 정책/연차 잔여일수를 academyId로 스코프하므로
    // (attendance는 이번 단일 학원 전환 범위에 포함되지 않음) 호출부에서 AuthUser.academyId()를 그대로 전달한다.
    void submit(Long documentId, Long academyId, Long requesterId, LocalDate startDate, LocalDate endDate,
                LocalDateTime submittedAt);
}
