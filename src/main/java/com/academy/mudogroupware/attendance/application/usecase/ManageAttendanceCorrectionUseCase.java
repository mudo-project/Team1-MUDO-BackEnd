package com.academy.mudogroupware.attendance.application.usecase;

import com.academy.mudogroupware.attendance.application.query.AdminAttendanceCorrectionView;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.global.domain.common.page.PageResult;

public interface ManageAttendanceCorrectionUseCase {
    PageResult<AdminAttendanceCorrectionView> getAll(
            AttendanceCorrectionStatus status, int page, int size);
    AdminAttendanceCorrectionView get(Long requestId);
    void approve(Long requestId, Long processorId);
    void reject(Long requestId, Long processorId, String reason);
}
