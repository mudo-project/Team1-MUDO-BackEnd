package com.academy.mudogroupware.attendance.application.usecase;
import java.util.List;
import com.academy.mudogroupware.attendance.application.query.AttendanceCorrectionView;
public interface GetMyAttendanceCorrectionUseCase {
    List<AttendanceCorrectionView> getAll(Long userId);
    AttendanceCorrectionView get(Long requestId, Long userId);
}
