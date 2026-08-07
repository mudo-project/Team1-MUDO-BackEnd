package com.academy.mudogroupware.attendance.presentation.api.response;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.academy.mudogroupware.attendance.application.query.MyAttendanceDayView;
public record MyAttendanceDayResponse(LocalDate date, LocalDateTime clockInAt, LocalDateTime clockOutAt,
        String clockInNote, String clockOutNote, boolean correctionRequestPending) {
    public static MyAttendanceDayResponse from(MyAttendanceDayView v) {
        return new MyAttendanceDayResponse(v.date(), v.clockInAt(), v.clockOutAt(), v.clockInNote(), v.clockOutNote(), v.correctionRequestPending());
    }
}
