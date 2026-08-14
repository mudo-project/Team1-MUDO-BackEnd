package com.academy.mudogroupware.rollcall.domain.repository;

import java.time.LocalDate;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendRecord;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public interface AttendanceMessageSendRecordRepository {

    /**
     * (lectureId, studentId, date, attendanceStatus) 조합의 발송 레코드가 없으면 PENDING 상태로 새로
     * 만들어 반환하고, 이미 있으면(동시 요청 등으로 먼저 생성된 경우 포함) 그 기존 레코드를 그대로
     * 반환한다. 유니크 제약을 이용해 원자적으로 처리되므로, 동시에 두 요청이 와도 하나만 새로 생성된다.
     * attendanceStatus를 키에 포함해, 출결 상태가 정정되면(예: 결석→지각) 새 조합으로 취급해
     * 재발송을 막지 않는다.
     */
    AttendanceMessageSendRecord createOrGetExisting(Long lectureId, Long studentId, LocalDate date,
                                                      AttendanceStatus attendanceStatus);

    /**
     * 레코드가 PENDING 또는 FAILED 상태일 때만 SENDING으로 전환해 "발송 권한"을 원자적으로 넘겨준다.
     * 두 요청이 같은 레코드를 동시에 봐도 SENDING 전환(UPDATE)은 하나만 성공하므로, 성공한 쪽만
     * 실제로 SmsSenderPort를 호출해야 한다. INDETERMINATE는 자동 재시도 대상에서 제외한다.
     */
    boolean claimForSending(Long id);

    AttendanceMessageSendRecord save(AttendanceMessageSendRecord record);
}
