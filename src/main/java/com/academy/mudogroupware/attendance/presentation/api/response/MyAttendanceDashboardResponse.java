package com.academy.mudogroupware.attendance.presentation.api.response;

import com.academy.mudogroupware.attendance.application.query.MyAttendanceDashboardView;

public record MyAttendanceDashboardResponse(
        MyMonthlyAttendanceResponse calendar,
        MyTodayAttendanceResponse today,
        MyLeaveSummaryResponse leave,
        MyEmploymentSummaryResponse employment) {

    public static MyAttendanceDashboardResponse from(MyAttendanceDashboardView view) {
        return new MyAttendanceDashboardResponse(
                MyMonthlyAttendanceResponse.from(view.calendar()),
                MyTodayAttendanceResponse.from(view.today()),
                MyLeaveSummaryResponse.from(view.leave()),
                MyEmploymentSummaryResponse.from(view.employment()));
    }
}
