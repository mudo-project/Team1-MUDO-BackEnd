package com.academy.mudogroupware.attendance.application.usecase;
import com.academy.mudogroupware.attendance.application.command.CreateAttendanceCorrectionCommand;
import com.academy.mudogroupware.attendance.application.query.AttendanceCorrectionView;
public interface CreateAttendanceCorrectionUseCase { AttendanceCorrectionView create(CreateAttendanceCorrectionCommand command); }
