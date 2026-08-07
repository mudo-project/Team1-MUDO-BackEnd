package com.academy.mudogroupware.attendance.application.usecase;
import java.time.LocalDate;
import com.academy.mudogroupware.attendance.application.query.MyAttendanceDayView;
public interface GetMyAttendanceDayUseCase { MyAttendanceDayView get(Long academyId, Long userId, LocalDate date); }
