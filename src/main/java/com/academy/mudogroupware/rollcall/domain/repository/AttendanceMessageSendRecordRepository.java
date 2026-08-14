package com.academy.mudogroupware.rollcall.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendRecord;

public interface AttendanceMessageSendRecordRepository {

    /**
     * (lectureId, studentId, date) 조합의 발송 레코드가 없으면 PENDING 상태로 새로 만들어 반환하고,
     * 이미 있으면(동시 요청 등으로 먼저 생성된 경우 포함) 그 기존 레코드를 그대로 반환한다.
     * 유니크 제약을 이용해 원자적으로 처리되므로, 동시에 두 요청이 와도 하나만 새로 생성된다.
     */
    AttendanceMessageSendRecord createOrGetExisting(Long lectureId, Long studentId, LocalDate date,
                                                      LocalDateTime now);

    AttendanceMessageSendRecord save(AttendanceMessageSendRecord record);
}
